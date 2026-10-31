package br.com.uaicortes

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/** Efeito temporal com movimento real de câmera por quadro (sem deformar a imagem). */
@UnstableApi
internal class AnimatedImageMatrix(private val index: Int, private val durationMs: Long,
                                   private val intensity: String) : MatrixTransformation {
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val fraction = (presentationTimeUs / 1000f / durationMs.coerceAtLeast(1L)).coerceIn(0f,1f)
        val f = ImageAnimationPlan.frame(index, fraction, intensity)
        return Matrix().apply {
            setScale(f.scale, f.scale)
            postTranslate(f.panX, f.panY)
        }
    }
}

/**
 * Exportador somente local: imagens normalizadas uma por vez, quadro fixo, GPU Media3,
 * MP4 H.264 no MediaStore. Sem manter bitmaps grandes na memória entre imagens.
 */
@UnstableApi
internal class ImageAnimationEngine(private val context: Context) {
    private val holder = ProgressHolder()
    private var transformer: Transformer? = null
    private var outputFile: File? = null
    private var workDir: File? = null
    private val cancelled = AtomicBoolean(false)
    @Volatile private var saving = false
    @Volatile private var preparing = false

    val isSaving: Boolean get() = saving
    fun progress(): Int? = transformer?.let {
        if (it.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress.coerceIn(0,100)
        else null
    }

    fun cancel() {
        if (saving) return // Cópia MediaStore tem rollback próprio e não pode parar pela metade.
        cancelled.set(true)
        transformer?.cancel()
        transformer = null
        if (!preparing) cleanup()
    }
    private fun cleanup() {
        outputFile?.delete()
        workDir?.deleteRecursively()
        outputFile = null
        workDir = null
    }

    suspend fun start(images: List<Uri>, options: ImageAnimationOptions,
                      onStage: (String) -> Unit,
                      onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        options.checked()
        require(images.isNotEmpty() && images.size <= ImageAnimationPlan.MAX_IMAGES)
        val reserveBytes = VideoExportQuality.minimumFreeBytes(
            ImageAnimationPlan.totalDuration(images.size, options), options.quality, false)
        require(context.cacheDir.usableSpace > reserveBytes) {
            "Espaço livre insuficiente para gerar o vídeo nesta qualidade. " +
                "Reduza a quantidade de fotos, selecione 720p ou libere armazenamento."
        }
        cancelled.set(false)
        saving = false
        cleanup()
        val folder = File(context.cacheDir, "animated-${System.nanoTime()}").also { it.mkdirs() }
        workDir = folder
        onStage("Preparando imagens")
        preparing = true
        val prepared = try {
            withContext(Dispatchers.IO) {
                images.mapIndexed { index, uri ->
                    coroutineContext.ensureActive()
                    if (cancelled.get()) throw CancellationException("Exportação cancelada")
                    // PNG temporário sem perdas; evita compressão JPEG antes do H.264.
                    val file = File(folder, "image-%03d.png".format(index))
                    composeImage(uri, file, options.width, options.height)
                    check(context.cacheDir.usableSpace > reserveBytes) {
                        "Espaço insuficiente após preparar as fotos. Use menos imagens ou libere espaço."
                    }
                    file
                }
            }
        } catch (e: Throwable) {
            cleanup()
            throw e
        } finally {
            preparing = false
        }
        if (cancelled.get()) { cleanup(); return }
        onStage("Exportando vídeo")
        val effects = prepared.mapIndexed { index, file ->
            val image = MediaItem.Builder().setUri(Uri.fromFile(file))
                .setImageDurationMs(options.durationMs).build()
            val videoEffects = mutableListOf<androidx.media3.common.Effect>(
                AnimatedImageMatrix(index, options.durationMs, options.intensity))
            val fade = ImageAnimationPlan.fadeMs(options)
            val incoming = if (index == 0) "Corte direto" else ImageAnimationPlan.transitionKind(options,index-1)
            val outgoing = if (index == prepared.lastIndex) "Corte direto"
                else ImageAnimationPlan.transitionKind(options,index)
            if (incoming != "Corte direto" || outgoing != "Corte direto")
                videoEffects += ClipColorTransitionEffect(options.durationMs,
                    incoming, fade, outgoing, fade)
            EditedMediaItem.Builder(image).setFrameRate(options.fps)
                .setEffects(Effects(emptyList(),videoEffects)).build()
        }
        val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO)).apply {
            effects.forEach { addItem(it) }
        }.build()
        val composition = Composition.Builder(listOf(sequence)).setEffects(Effects(emptyList(),
            listOf(Presentation.createForWidthAndHeight(options.width,options.height,
                Presentation.LAYOUT_SCALE_TO_FIT)))).build()
        val target = File(folder,"render.mp4")
        outputFile = target
        val requestedBitrate = VideoExportQuality.videoBitrateBps(options.quality)
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder()
                .setBitrate(requestedBitrate).build())
            .setEnableFallback(true)
            .build()
        transformer = Transformer.Builder(context).setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    android.util.Log.i("UaiCortesAnimation", "MP4: ${result.width}x${result.height}, " +
                        "bitrate medido=${result.averageVideoBitrate} bps, solicitado=$requestedBitrate bps")
                    transformer = null
                    saving = true
                    onStage("Salvando na galeria")
                    Thread {
                        try {
                            val uri = saveToGallery(target)
                            Handler(Looper.getMainLooper()).post { onSaved(uri) }
                        } catch (e: Exception) {
                            DiagnosticRecorder.exception("animated_images_gallery",e)
                            Handler(Looper.getMainLooper()).post { onError(e.message ?: "Falha ao salvar") }
                        } finally {
                            saving = false
                            cleanup()
                        }
                    }.start()
                }
                override fun onError(composition: Composition, result: ExportResult,
                                     e: ExportException) {
                    DiagnosticRecorder.exception("animated_images_export",e)
                    onError("Falha ao gerar MP4 (${e.errorCodeName}). ${e.cause?.message ?: e.message.orEmpty()}")
                    transformer = null
                    cleanup()
                }
            }).build()
        coroutineContext.ensureActive()
        if (cancelled.get()) { transformer = null; cleanup(); return }
        try { transformer?.start(composition,target.absolutePath) }
        catch (e: Exception) { transformer=null;cleanup();throw e }
    }

    private fun composeImage(uri: Uri, output: File, width: Int, height: Int) {
        // A imagem original é aberta apenas sob demanda. URI pode ser MediaStore ou SAF.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (context.contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Imagem indisponível. Selecione-a novamente.")).use {
            BitmapFactory.decodeStream(it,null,bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagem inválida ou incompatível" }
        var sample = 1
        while (bounds.outWidth / sample > width*2 || bounds.outHeight / sample > height*2) sample *= 2
        val opt = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,opt) }
            ?: throw IllegalArgumentException("Não foi possível decodificar a imagem")
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val rotation = when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90f
            ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180f
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270f
            else -> 0f
        }
        val flip = orientation in setOf(ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            ExifInterface.ORIENTATION_TRANSPOSE,ExifInterface.ORIENTATION_TRANSVERSE)
        val img = if (rotation != 0f || flip) {
            try {
                val matrix = Matrix().apply {
                    if (flip) postScale(-1f,1f)
                    if (rotation!=0f) postRotate(rotation)
                }
                Bitmap.createBitmap(decoded,0,0,decoded.width,decoded.height,matrix,true)
            } finally { decoded.recycle() }
        } else decoded
        try {
            val canvasBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
            try {
                val c = Canvas(canvasBitmap)
                // Fundo com cores derivadas da própria foto: miniatura ampliada suavemente,
                // para não deixar tarjas pretas e sem distorcer a pessoa/foto em destaque.
                c.drawColor(Color.BLACK)
                val scaleFill = maxOf(width/img.width.toFloat(),height/img.height.toFloat())
                val bgW = img.width*scaleFill; val bgH = img.height*scaleFill
                c.drawBitmap(img,null,android.graphics.RectF((width-bgW)/2,(height-bgH)/2,
                    (width+bgW)/2,(height+bgH)/2),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                c.drawColor(0x99000000.toInt())
                val fit = minOf(width/img.width.toFloat(),height/img.height.toFloat())
                val fgW = img.width*fit; val fgH=img.height*fit
                c.drawBitmap(img,null,android.graphics.RectF((width-fgW)/2,(height-fgH)/2,
                    (width+fgW)/2,(height+fgH)/2),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                output.outputStream().buffered().use { stream ->
                    check(canvasBitmap.compress(Bitmap.CompressFormat.PNG,100,stream))
                }
            } finally { canvasBitmap.recycle() }
        } finally { img.recycle() }
    }

    private fun saveToGallery(file: File): Uri = GalleryVideoSaver.save(
        context, file, "UaiCortes_Animado_${System.currentTimeMillis()}.mp4"
    )

}
