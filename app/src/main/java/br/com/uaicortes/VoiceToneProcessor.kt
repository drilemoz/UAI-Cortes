package br.com.uaicortes

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Usa um único passo PCM para equalizar, comprimir e aplicar ganho, sem clipping entre filtros. */
@OptIn(UnstableApi::class)
internal class VoiceToneProcessor(private val tone: VoiceTone, private val volume: Float = 1f) : BaseAudioProcessor() {
    private var dsp: VoiceToneDsp? = null
    private var channelCount = 0
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..8 ||
            inputAudioFormat.sampleRate < 8000)
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        channelCount = inputAudioFormat.channelCount
        dsp = VoiceToneDsp(tone, inputAudioFormat.sampleRate, channelCount)
        return inputAudioFormat
    }
    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) { dsp?.reset() }
    override fun onReset() { dsp = null; channelCount = 0 }
    override fun queueInput(inputBuffer: ByteBuffer) {
        inputBuffer.order(ByteOrder.nativeOrder())
        val out = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        val samples = DoubleArray(channelCount)
        while (inputBuffer.remaining() >= channelCount * 2) {
            for (ch in 0 until channelCount) samples[ch] = inputBuffer.short / 32768.0
            dsp!!.process(samples)
            for (ch in 0 until channelCount) {
                val scaled = (samples[ch] * volume.coerceIn(0f,1f) * 32768.0)
                    .roundToInt().coerceIn(-32768,32767)
                out.putShort(scaled.toShort())
            }
        }
        out.flip()
    }
}

/** Prévia isolada com o mesmo DSP usado pelo Media3 Transformer na exportação. */
@OptIn(UnstableApi::class)
internal fun voiceTonePreviewPlayer(context: Context, tone: VoiceTone): ExoPlayer {
    if (!tone.enabled) return diagnosticPlayer(context)
    val renderers = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(context: Context, enableFloatOutput: Boolean,
                                    enableAudioOutputPlaybackParams: Boolean): AudioSink =
            DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setAudioProcessors(arrayOf<AudioProcessor>(VoiceToneProcessor(tone)))
                .build()
    }
    return ExoPlayer.Builder(context, renderers).build()
}
