package br.com.uaicortes

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

/** Salva uma faixa M4A na galeria. Uma faixa incompleta permanece oculta (IS_PENDING). */
internal object QuickSplitAudioStore {
    fun createPending(context: Context, name: String): Uri {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "A extração automática de áudio por parte exige Android 10 ou superior."
        }
        val metadata = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/UaiCortes/Divididos")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, metadata)
            ?: error("Não foi possível criar o arquivo de áudio.")
    }

    fun finish(context: Context, uri: Uri) {
        val updated = context.contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.Audio.Media.IS_PENDING, 0)
        }, null, null)
        check(updated > 0) { "Falha ao finalizar o M4A na galeria." }
    }

    fun discard(context: Context, uri: Uri) { runCatching { context.contentResolver.delete(uri, null, null) } }
}

/** Divide um único vídeo em MP4s independentes, um Transformer ativo por vez. */
@androidx.annotation.OptIn(UnstableApi::class)
internal class QuickVideoSplitEngine(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var transformer: Transformer? = null
    private var audioEngine: AudioExtractionEngine? = null
    private var temp: File? = null
    private var activeGeneration = 0
    private var saving = false
    private var pendingAudio: Uri? = null

    fun cancel() {
        activeGeneration++
        transformer?.cancel()
        transformer = null
        audioEngine?.cancel()
        audioEngine = null
        pendingAudio?.let { QuickSplitAudioStore.discard(context, it) }
        pendingAudio = null
        if (!saving) temp?.delete()
        temp = null
    }

    fun start(
        source: Uri,
        originalName: String,
        parts: List<QuickVideoPart>,
        audioMode: String,
        onProgress: (Int, Int, String) -> Unit,
        onDone: (Int) -> Unit,
        onError: (String, Int) -> Unit
    ) {
        require(audioMode in listOf("keep", "mute", "separate"))
        require(parts.isNotEmpty() && parts.size <= QuickVideoSplitPolicy.MAX_PARTS)
        cancel()
        val generation = activeGeneration
        val fileBase = QuickVideoSplitPolicy.fileBase(originalName)
        val session = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.ROOT)
            .format(java.util.Date())
        var completed = 0
        fun isCurrent() = generation == activeGeneration

        fun fail(reason: String) {
            if (!isCurrent()) return
            transformer = null
            audioEngine = null
            saving = false
            temp?.delete(); temp = null
            onError(reason, completed)
        }

        fun nextPart(index: Int) {
            if (!isCurrent()) return
            if (index >= parts.size) {
                transformer = null
                audioEngine = null
                temp = null
                onDone(completed)
                return
            }
            val part = parts[index]
            onProgress(index + 1, parts.size, "Gerando vídeo")
            val output = File(context.cacheDir, "uai-dividir-${System.nanoTime()}-${part.index}.mp4")
            temp = output
            val clip = MediaItem.Builder().setUri(source).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(part.startMs)
                    .setEndPositionMs(part.endMs)
                    .build()
            ).build()
            val item = EditedMediaItem.Builder(clip)
                .setRemoveAudio(audioMode == "mute")
                .build()
            val fileName = QuickVideoSplitPolicy.partName(fileBase, session, part.index, "mp4")
            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (!isCurrent()) { output.delete(); return }
                        transformer = null
                        saving = true
                        onProgress(index + 1, parts.size, "Salvando MP4")
                        Thread {
                            val result = runCatching {
                                GalleryVideoSaver.save(context, output, fileName, subfolder = "Divididos")
                            }
                            output.delete()
                            main.post {
                                saving = false
                                if (!isCurrent()) return@post
                                temp = null
                                result.onFailure { error ->
                                    fail(error.message ?: "Não foi possível salvar o MP4.")
                                }.onSuccess {
                                    if (audioMode != "separate") {
                                        completed++
                                        nextPart(index + 1)
                                    } else {
                                        onProgress(index + 1, parts.size, "Extraindo áudio M4A")
                                        val audioName = QuickVideoSplitPolicy.partName(fileBase, session, part.index, "m4a")
                                        val destination = runCatching {
                                            QuickSplitAudioStore.createPending(context, audioName)
                                        }.getOrElse { error -> fail(error.message ?: "Não foi possível salvar M4A."); return@onSuccess }
                                        pendingAudio = destination
                                        val audioClip = Clip(uri = source.toString(), title = originalName,
                                            sourceDurationMs = parts.last().endMs,
                                            startMs = part.startMs, endMs = part.endMs)
                                        audioEngine = AudioExtractionEngine(context).also { engine ->
                                            runCatching {
                                                engine.start(listOf(audioClip), destination,
                                                    onSuccess = {
                                                        if (!isCurrent()) {
                                                            QuickSplitAudioStore.discard(context, destination)
                                                        } else {
                                                            val completedAudio = runCatching {
                                                                QuickSplitAudioStore.finish(context, destination)
                                                            }
                                                            pendingAudio = null
                                                            audioEngine = null
                                                            completedAudio.onFailure { err ->
                                                                QuickSplitAudioStore.discard(context, destination)
                                                                fail(err.message ?: "Não foi possível finalizar M4A.")
                                                            }.onSuccess {
                                                                completed++
                                                                nextPart(index + 1)
                                                            }
                                                        }
                                                    },
                                                    onError = { reason ->
                                                        QuickSplitAudioStore.discard(context, destination)
                                                        pendingAudio = null
                                                        fail(reason)
                                                    })
                                            }.onFailure { error ->
                                                QuickSplitAudioStore.discard(context, destination)
                                                pendingAudio = null
                                                fail(error.message ?: "Falha ao extrair áudio.")
                                            }
                                        }
                                    }
                                }
                            }
                        }.start()
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult,
                                         exportException: ExportException) {
                        output.delete()
                        fail(exportException.message ?: "Falha no Media3 Transformer.")
                    }
                }).build()
            runCatching { transformer!!.start(item, output.absolutePath) }
                .onFailure { error -> output.delete(); fail(error.message ?: "Não foi possível processar o vídeo.") }
        }
        nextPart(0)
    }
}
