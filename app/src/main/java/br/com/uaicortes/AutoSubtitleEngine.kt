package br.com.uaicortes

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

/** Modelo PT pequeno oficial, baixado somente após ação explícita do usuário.
 * A transcrição e o áudio nunca são enviados pelo aplicativo à rede.
 */
object AutoSubtitleEngine {
    const val MODEL_NAME = "vosk-model-small-pt-0.3"
    const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip"
    private const val MAX_DOWNLOAD = 90L * 1024 * 1024
    private const val MAX_EXTRACTED = 300L * 1024 * 1024
    private const val MAX_DURATION_MS = 10L * 60 * 1000
    private const val RATE = 16000
    private val installationMutex = Mutex()

    fun modelFolder(context: Context): File = File(context.filesDir, "speech-models/$MODEL_NAME")
    fun installed(context: Context): Boolean {
        return VoskModelLayout.detect(modelFolder(context)) != null
    }

    /** O download permanece opcional. Relata bytes reais mesmo antes de completar 1%. */
    suspend fun install(
        context: Context,
        onProgress: suspend (Int?) -> Unit,
        onStage: suspend (String) -> Unit = {},
        onTransfer: suspend (Long, Long?) -> Unit = { _, _ -> }
    ) = installationMutex.withLock {
        withContext(Dispatchers.IO) {
            if (installed(context)) return@withContext
            withStaging(context) { zip, unpack, target ->
                onStage("Conectando ao servidor oficial…")
                val conn = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept-Encoding", "identity")
                }
                try {
                    conn.connect()
                    if (conn.url.protocol != "https") error("Redirecionamento inseguro do download")
                    if (conn.responseCode !in 200..299)
                        error("Servidor retornou HTTP ${conn.responseCode}")
                    val total = conn.contentLengthLong.takeIf { it > 0 }
                    if (total != null && total > MAX_DOWNLOAD) error("Modelo ultrapassa o limite permitido")
                    onStage("Baixando modelo Vosk…")
                    conn.inputStream.use { input ->
                        copyArchive(input, zip, total, onProgress, onTransfer)
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    throw IOException("Servidor demorou a responder. Tente baixar no navegador e importar o ZIP.", e)
                } catch (e: java.net.UnknownHostException) {
                    throw IOException("Não foi possível localizar o servidor. Verifique DNS e conexão.", e)
                } finally {
                    conn.disconnect()
                }
                extractAndInstall(zip, unpack, target, onStage)
                onProgress(100)
            }
        }
    }

    /** Alternativa offline: importa ZIP oficial previamente baixado via seletor SAF. */
    suspend fun importZip(
        context: Context,
        uri: Uri,
        onProgress: suspend (Int?) -> Unit,
        onStage: suspend (String) -> Unit = {},
        onTransfer: suspend (Long, Long?) -> Unit = { _, _ -> }
    ) = installationMutex.withLock {
        withContext(Dispatchers.IO) {
            if (installed(context)) return@withContext
            withStaging(context) { zip, unpack, target ->
                onStage("Copiando arquivo selecionado…")
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Não foi possível abrir o arquivo selecionado")
                input.use { copyArchive(it, zip, null, onProgress, onTransfer) }
                extractAndInstall(zip, unpack, target, onStage)
                onProgress(100)
            }
        }
    }

    private suspend fun withStaging(
        context: Context,
        action: suspend (File, File, File) -> Unit
    ) {
        val base = File(context.filesDir, "speech-models")
        if (!base.isDirectory && !base.mkdirs()) error("Não foi possível criar pasta do modelo")
        val zip = File(base, "$MODEL_NAME.tmp.zip")
        val unpack = File(base, "$MODEL_NAME.unpack")
        try { action(zip, unpack, modelFolder(context)) }
        finally { zip.delete(); unpack.deleteRecursively() }
    }

    private suspend fun copyArchive(
        input: InputStream,
        destination: File,
        expected: Long?,
        onProgress: suspend (Int?) -> Unit,
        onTransfer: suspend (Long, Long?) -> Unit
    ) {
        var count = 0L
        var lastReport = -1L
        var lastPercent = -2
        onProgress(null)
        onTransfer(0L, expected)
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = input.read(buffer)
                if (n < 0) break
                if (n == 0) continue
                count += n
                if (count > MAX_DOWNLOAD) error("ZIP maior que o limite permitido (90 MiB)")
                output.write(buffer, 0, n)
                val percent = ModelTransferProgress.percent(count, expected)
                if (percent != lastPercent) {
                    onProgress(percent)
                    lastPercent = percent ?: -1
                }
                if (count - lastReport >= 256 * 1024 || lastReport < 0L) {
                    onTransfer(count, expected)
                    lastReport = count
                }
            }
        }
        onTransfer(count, expected)
        if (expected != null && count != expected)
            error("Download incompleto: recebidos $count de $expected bytes")
        if (count < 1024L) error("Arquivo ZIP vazio ou incompleto")
    }

    private suspend fun extractAndInstall(
        zip: File,
        unpack: File,
        target: File,
        onStage: suspend (String) -> Unit
    ) {
        onStage("Descompactando e verificando o modelo…")
        unpack.deleteRecursively()
        if (!unpack.mkdirs()) error("Não foi possível preparar pasta temporária")
        val safeRoot = unpack.canonicalFile
        var extracted = 0L
        var entries = 0
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = input.nextEntry ?: break
                entries++
                if (entries > 4000) error("Muitos arquivos no ZIP")
                val relative = entry.name.removePrefix("$MODEL_NAME/")
                if (relative == entry.name || relative.isBlank()) {
                    input.closeEntry()
                    continue
                }
                val destination = File(safeRoot, relative).canonicalFile
                if (!destination.path.startsWith(safeRoot.path + File.separator))
                    error("Caminho inseguro dentro do ZIP")
                if (entry.isDirectory) destination.mkdirs() else {
                    destination.parentFile?.mkdirs()
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            if (n == 0) continue
                            extracted += n
                            if (extracted > MAX_EXTRACTED) error("Modelo descompactado grande demais")
                            output.write(buffer, 0, n)
                        }
                    }
                }
                input.closeEntry()
            }
        }
        if (entries == 0 || VoskModelLayout.detect(unpack) == null)
            error("ZIP não contém um modelo Vosk válido. Verifique o arquivo $MODEL_NAME.")
        if (VoskModelLayout.detect(target) != null) return
        // Só troca instalação antiga quando o pacote novo foi validado.
        if (target.exists() && !target.deleteRecursively()) error("Falha ao remover instalação incompleta")
        if (!unpack.renameTo(target)) error("Não foi possível instalar o modelo")
    }

    private data class Word(val text: String, val startMs: Long, val endMs: Long)
    internal fun parsedWords(result: String): List<Triple<String, Long, Long>> {
        val array = runCatching { JSONObject(result).optJSONArray("result") }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val j = array.optJSONObject(index) ?: return@mapNotNull null
            val value = j.optString("word").trim().take(70)
            if (value.isBlank()) null else Triple(value,
                (j.optDouble("start", 0.0) * 1000).toLong(),
                (j.optDouble("end", 0.0) * 1000).toLong())
        }
    }

    private fun segment(words: List<Word>, total: Long): List<SubtitleCue> {
        val result = mutableListOf<SubtitleCue>()
        var segment = mutableListOf<Word>()
        fun flush() {
            if (segment.isEmpty()) return
            val start = segment.first().startMs.coerceIn(0, total)
            val end = segment.last().endMs.coerceIn((start + 250).coerceAtMost(total), total)
            if (end > start) result += SubtitleCue(text = segment.joinToString(" ") { it.text }.take(170),
                startMs = start, endMs = end)
            segment = mutableListOf()
        }
        for (word in words) {
            if (segment.isNotEmpty() && (segment.size >= 7 ||
                    word.startMs - segment.last().endMs > 750L ||
                    word.endMs - segment.first().startMs > 3600L)) flush()
            segment += word
        }
        flush()
        return result.take(600)
    }

    /** MediaCodec decodifica áudio embutido no vídeo, sem abrir microfone.
     * Usa mono 16k PCM para Vosk, com reamostragem simples e sem rede.
     * Cada clipe recebe horários próprios para respeitar cortes e junções.
     */
    suspend fun transcribe(context: Context, project: VideoProject,
        onProgress: suspend (Int, String) -> Unit): List<SubtitleCue> = withContext(Dispatchers.IO) {
        require(installed(context)) { "Baixe o modelo de português primeiro" }
        require(project.clips.isNotEmpty()) { "Projeto sem vídeo" }
        require(project.durationMs in 1..MAX_DURATION_MS) {
            "A transcrição offline aceita até 10 minutos por operação; divida vídeos maiores."
        }
        val all = mutableListOf<Word>()
        val model = Model(modelFolder(context).absolutePath)
        try {
            var timeline = 0L
            project.clips.forEachIndexed { index, clip ->
                currentCoroutineContext().ensureActive()
                onProgress(index * 100 / project.clips.size,
                    "Reconhecendo áudio do clipe ${index+1} de ${project.clips.size}")
                all += readClip(context, clip, timeline, model)
                timeline += clip.durationMs
                onProgress((index + 1) * 100 / project.clips.size,
                    "Clipe ${index+1} de ${project.clips.size} analisado")
            }
            segment(all, project.durationMs)
        } finally { model.close() }
    }

    private suspend fun readClip(context: Context, clip: Clip, timeline: Long, model: Model): List<Word> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(clip.uri), null)
            val selected = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return emptyList()
            extractor.selectTrack(selected)
            val format = extractor.getTrackFormat(selected)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            // Solicita PCM16, verificando o formato efetivamente produzido pelo decoder.
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            extractor.seekTo(clip.startMs.coerceAtLeast(0) * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            val results = mutableListOf<Word>()
            val outputChunk = java.io.ByteArrayOutputStream(4096)
            var phase = 0L
            val startUs = clip.startMs * 1000L
            val endUs = clip.endMs * 1000L
            val info = MediaCodec.BufferInfo()
            val recognizer = Recognizer(model, RATE.toFloat())
            try {
                recognizer.setWords(true)
                fun absorbResult(text: String) {
                    parsedWords(text).forEach { (word, start, end) ->
                        results += Word(word, timeline + start, timeline + end)
                    }
                }
                fun feed() {
                    if (outputChunk.size() == 0) return
                    val bytes = outputChunk.toByteArray()
                    if (recognizer.acceptWaveForm(bytes, bytes.size)) absorbResult(recognizer.result)
                    outputChunk.reset()
                }
                var inputEnded = false
                var outputEnded = false
                while (!outputEnded) {
                    currentCoroutineContext().ensureActive()
                    if (!inputEnded) {
                        val index = decoder.dequeueInputBuffer(12000L)
                        if (index >= 0) {
                            val buffer = decoder.getInputBuffer(index) ?: error("Decoder sem buffer")
                            buffer.clear()
                            val size = extractor.readSampleData(buffer, 0)
                            val timestamp = extractor.sampleTime
                            if (size < 0 || timestamp >= endUs) {
                                decoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                decoder.queueInputBuffer(index, 0, size, timestamp.coerceAtLeast(0), 0)
                                extractor.advance()
                            }
                        }
                    }
                    when (val index = decoder.dequeueOutputBuffer(info, 12000L)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outputFormat = decoder.outputFormat
                            rate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
                            channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                            encoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
                                outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            else AudioFormat.ENCODING_PCM_16BIT
                        }
                        in 0..Int.MAX_VALUE -> {
                            val data = decoder.getOutputBuffer(index)
                            if (data != null && info.size > 0) {
                                data.order(ByteOrder.LITTLE_ENDIAN)
                                data.position(info.offset)
                                data.limit(info.offset + info.size)
                                val width = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                                if (encoding != AudioFormat.ENCODING_PCM_16BIT &&
                                    encoding != AudioFormat.ENCODING_PCM_FLOAT)
                                    error("Formato de áudio PCM não suportado neste aparelho")
                                val frames = info.size / (width * channels.coerceAtLeast(1))
                                for (frame in 0 until frames) {
                                    var average = 0f
                                    repeat(channels) {
                                        average += if (encoding == AudioFormat.ENCODING_PCM_FLOAT)
                                            data.float.coerceIn(-1f, 1f) * 32767f
                                        else data.short.toFloat()
                                    }
                                    val timestamp = info.presentationTimeUs + frame * 1000000L / rate
                                    if (timestamp < startUs || timestamp >= endUs) continue
                                    phase += RATE.toLong()
                                    while (phase >= rate) {
                                        phase -= rate
                                        val sample = (average / channels).toInt().coerceIn(-32768, 32767)
                                        outputChunk.write(sample and 0xFF)
                                        outputChunk.write((sample shr 8) and 0xFF)
                                        if (outputChunk.size() >= 4096) feed()
                                    }
                                }
                            }
                            decoder.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputEnded = true
                        }
                    }
                }
                feed()
                absorbResult(recognizer.finalResult)
                return results
            } finally { recognizer.close() }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }
}
