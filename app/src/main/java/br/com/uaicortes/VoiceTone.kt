package br.com.uaicortes

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/** Configuração não destrutiva, vinculada à fonte. Presets não alteram pitch/formantes. */
data class VoiceTone(
    val preset: String = OFF,
    val intensity: Float = 0.6f,
    val bassDb: Float = 0f,
    val bodyDb: Float = 0f,
    val clarityDb: Float = 0f
) {
    fun safe(): VoiceTone = copy(
        preset = preset.takeIf { it in setOf(OFF, NATURAL, WARM, CLEAR) } ?: OFF,
        intensity = intensity.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.6f,
        bassDb = bassDb.takeIf { it.isFinite() }?.coerceIn(-4f, 4f) ?: 0f,
        bodyDb = bodyDb.takeIf { it.isFinite() }?.coerceIn(-4f, 4f) ?: 0f,
        clarityDb = clarityDb.takeIf { it.isFinite() }?.coerceIn(-4f, 4f) ?: 0f
    )
    val enabled: Boolean get() = preset != OFF && intensity > 0f
    fun json(): JSONObject = JSONObject().put("preset", safe().preset)
        .put("intensity", safe().intensity.toDouble())
        .put("bassDb", safe().bassDb.toDouble())
        .put("bodyDb", safe().bodyDb.toDouble())
        .put("clarityDb", safe().clarityDb.toDouble())

    companion object {
        const val OFF = "off"
        const val NATURAL = "natural"
        const val WARM = "warm"
        const val CLEAR = "clear"
        fun read(json: JSONObject?): VoiceTone = if (json == null) VoiceTone() else VoiceTone(
            json.optString("preset", OFF), json.optDouble("intensity", .6).toFloat(),
            json.optDouble("bassDb", 0.0).toFloat(),
            json.optDouble("bodyDb", 0.0).toFloat(),
            json.optDouble("clarityDb", 0.0).toFloat()
        ).safe()
    }
}

/** Filtros RBJ por canal e compressor de dinâmica suave; PCM normalizado [-1,+1]. */
internal class VoiceToneDsp(settings: VoiceTone, private val sampleRate: Int, private val channels: Int) {
    private val tone = settings.safe()
    private val strength = tone.intensity.toDouble()
    private val bass = when (tone.preset) {
        VoiceTone.WARM -> 2.0
        VoiceTone.CLEAR -> -0.3
        else -> 0.7
    } * strength + tone.bassDb
    private val body = when (tone.preset) {
        VoiceTone.WARM -> 1.1
        VoiceTone.CLEAR -> -1.4
        else -> -0.5
    } * strength + tone.bodyDb
    private val clarity = when (tone.preset) {
        VoiceTone.WARM -> 0.3
        VoiceTone.CLEAR -> 2.0
        else -> 1.1
    } * strength + tone.clarityDb

    private val highPass = Array(channels) { Biquad.highPass(sampleRate, 72.0, 0.707) }
    private val lowShelf = Array(channels) { Biquad.lowShelf(sampleRate, 165.0, bass) }
    private val bodyPeak = Array(channels) { Biquad.peak(sampleRate, 320.0, 1.0, body) }
    private val clarityPeak = Array(channels) { Biquad.peak(sampleRate, 3200.0, 0.85, clarity) }
    private val attackCoefficient = exp(-1.0 / (sampleRate * 0.010))
    private val releaseCoefficient = exp(-1.0 / (sampleRate * 0.130))
    private val compressorThreshold = 10.0.pow(-21.0 / 20.0)
    private var envelope = 0.0
    private val frame = DoubleArray(channels)

    init { require(sampleRate >= 8000 && channels in 1..8) }

    /** Processa um quadro intercalado, sem alocações por amostra. Retorna o mesmo array. */
    fun process(samples: DoubleArray): DoubleArray {
        require(samples.size == channels)
        var peak = 0.0
        for (ch in 0 until channels) {
            var v = samples[ch].coerceIn(-1.0, 1.0)
            v = highPass[ch].apply(v)
            v = lowShelf[ch].apply(v)
            v = bodyPeak[ch].apply(v)
            v = clarityPeak[ch].apply(v)
            frame[ch] = v
            peak = max(peak, abs(v))
        }
        val coefficient = if (peak > envelope) attackCoefficient else releaseCoefficient
        envelope = coefficient * envelope + (1.0 - coefficient) * peak
        val ratio = 1.0 + 0.7 * strength
        val reduction = if (envelope > compressorThreshold)
            (compressorThreshold / envelope).pow(1.0 - 1.0 / ratio) else 1.0
        // Margem para realces do EQ; limitador de pico seguro no domínio PCM.
        val headroom = 10.0.pow((-1.5 * strength) / 20.0)
        for (ch in 0 until channels) {
            samples[ch] = (frame[ch] * reduction * headroom).coerceIn(-0.97, 0.97)
        }
        return samples
    }

    fun reset() {
        highPass.forEach { it.reset() }
        lowShelf.forEach { it.reset() }
        bodyPeak.forEach { it.reset() }
        clarityPeak.forEach { it.reset() }
        envelope = 0.0
    }
}

/** Biquad Direct Form II transposed, coeficientes normalizados. */
private class Biquad(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double
) {
    private var z1 = 0.0
    private var z2 = 0.0
    fun apply(x: Double): Double {
        val out = b0 * x + z1
        z1 = b1 * x - a1 * out + z2
        z2 = b2 * x - a2 * out
        return if (out.isFinite()) out else 0.0
    }
    fun reset() { z1 = 0.0; z2 = 0.0 }
    companion object {
        private fun normalized(b0: Double, b1: Double, b2: Double,
                               a0: Double, a1: Double, a2: Double) =
            Biquad(b0/a0,b1/a0,b2/a0,a1/a0,a2/a0)
        private fun omega(sr: Int, hz: Double) = 2.0 * PI * hz.coerceAtMost(sr * 0.42) / sr
        fun highPass(sr: Int, hz: Double, q: Double): Biquad {
            val w = omega(sr,hz); val c = cos(w); val a = sin(w)/(2*q)
            return normalized((1+c)/2,-(1+c),(1+c)/2,1+a,-2*c,1-a)
        }
        fun peak(sr: Int, hz: Double, q: Double, db: Double): Biquad {
            val w = omega(sr,hz); val c=cos(w); val alpha=sin(w)/(2*q); val a=10.0.pow(db/40)
            return normalized(1+alpha*a,-2*c,1-alpha*a,1+alpha/a,-2*c,1-alpha/a)
        }
        fun lowShelf(sr: Int, hz: Double, db: Double): Biquad {
            val w = omega(sr,hz); val c=cos(w); val s=sin(w)
            val a=10.0.pow(db/40)
            val alpha=s/2*sqrt(2.0) // shelf slope = 1
            val delta=2*sqrt(a)*alpha
            return normalized(a*((a+1)-(a-1)*c+delta),2*a*((a-1)-(a+1)*c),
                a*((a+1)-(a-1)*c-delta),(a+1)+(a-1)*c+delta,
                -2*((a-1)+(a+1)*c),(a+1)+(a-1)*c-delta)
        }
    }
}
