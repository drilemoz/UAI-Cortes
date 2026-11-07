package br.com.uaicortes

import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ajustes não destrutivos por fonte. O projeto antigo continua legível sem migração.
 * Não altera pitch, formantes nem faz separação de voz/música.
 */
data class VoiceTone(
    val preset: String = OFF,
    val intensity: Float = 0.6f,
    val bassDb: Float = 0f,
    val bodyDb: Float = 0f,
    val clarityDb: Float = 0f,
    val trebleDb: Float = 0f,
    val compression: Float = 0.55f,
    val noiseReduction: Float = 0f,
    val deEsser: Float = 0.35f,
    val autoGain: Boolean = true
) {
    fun safe(): VoiceTone = copy(
        preset = preset.takeIf { it in setOf(OFF, NATURAL, WARM, CLEAR, STUDIO) } ?: OFF,
        intensity = intensity.valid(0.6f, 0f, 1f),
        bassDb = bassDb.valid(0f, -6f, 6f),
        bodyDb = bodyDb.valid(0f, -6f, 6f),
        clarityDb = clarityDb.valid(0f, -6f, 6f),
        trebleDb = trebleDb.valid(0f, -6f, 6f),
        compression = compression.valid(0.55f, 0f, 1f),
        noiseReduction = noiseReduction.valid(0f, 0f, 1f),
        deEsser = deEsser.valid(0.35f, 0f, 1f)
    )
    val enabled: Boolean get() = preset != OFF && intensity > 0f
    fun json(): JSONObject = safe().let { value ->
        JSONObject().put("preset", value.preset)
            .put("intensity", value.intensity.toDouble())
            .put("bassDb", value.bassDb.toDouble())
            .put("bodyDb", value.bodyDb.toDouble())
            .put("clarityDb", value.clarityDb.toDouble())
            .put("trebleDb", value.trebleDb.toDouble())
            .put("compression", value.compression.toDouble())
            .put("noiseReduction", value.noiseReduction.toDouble())
            .put("deEsser", value.deEsser.toDouble())
            .put("autoGain", value.autoGain)
    }
    companion object {
        const val OFF = "off"
        const val NATURAL = "natural"
        const val WARM = "warm"
        const val CLEAR = "clear"
        const val STUDIO = "studio"

        fun read(json: JSONObject?): VoiceTone = if (json == null) VoiceTone() else VoiceTone(
            preset = json.optString("preset", OFF),
            intensity = json.optDouble("intensity", 0.6).toFloat(),
            bassDb = json.optDouble("bassDb", 0.0).toFloat(),
            bodyDb = json.optDouble("bodyDb", 0.0).toFloat(),
            clarityDb = json.optDouble("clarityDb", 0.0).toFloat(),
            trebleDb = json.optDouble("trebleDb", 0.0).toFloat(),
            compression = json.optDouble("compression", 0.55).toFloat(),
            noiseReduction = json.optDouble("noiseReduction", 0.0).toFloat(),
            deEsser = json.optDouble("deEsser", 0.35).toFloat(),
            autoGain = json.optBoolean("autoGain", true)
        ).safe()
    }
}

private fun Float.valid(default: Float, lower: Float, upper: Float): Float =
    if (isFinite()) coerceIn(lower, upper) else default

/**
 * EQ RBJ, compressor de ganho vinculado aos canais, expander suave opcional,
 * de-esser de banda estreita e limitador de picos de amostra. NÃO é true peak,
 * nem RNNoise. Cada instância pertence a um único stream PCM.
 */
internal class VoiceToneDsp(settings: VoiceTone, private val sampleRate: Int, private val channels: Int) {
    private val tone = settings.safe()
    private val strength = tone.intensity.toDouble()
    private val bass = (when (tone.preset) {
        VoiceTone.WARM -> 4.0
        VoiceTone.STUDIO -> 1.6
        VoiceTone.CLEAR -> -0.4
        else -> 1.2
    } + tone.bassDb) * strength
    private val body = (when (tone.preset) {
        VoiceTone.WARM -> 2.7
        VoiceTone.STUDIO -> 0.8
        VoiceTone.CLEAR -> -1.4
        else -> 0.2
    } + tone.bodyDb) * strength
    private val clarity = (when (tone.preset) {
        VoiceTone.WARM -> 1.0
        VoiceTone.STUDIO -> 2.3
        VoiceTone.CLEAR -> 3.4
        else -> 1.5
    } + tone.clarityDb) * strength
    private val treble = (when (tone.preset) {
        VoiceTone.CLEAR -> 1.0
        VoiceTone.STUDIO -> 0.5
        else -> 0.0
    } + tone.trebleDb) * strength

    private val highPass = Array(channels) { VoiceBiquad.highPass(sampleRate, 75.0, 0.707) }
    private val lowShelf = Array(channels) { VoiceBiquad.lowShelf(sampleRate, 155.0, bass) }
    private val bodyPeak = Array(channels) { VoiceBiquad.peak(sampleRate, 260.0, 0.9, body) }
    private val clarityPeak = Array(channels) { VoiceBiquad.peak(sampleRate, 3000.0, 0.85, clarity) }
    private val highShelf = Array(channels) { VoiceBiquad.highShelf(sampleRate, 7600.0, treble) }
    private val sibilanceBand = if (sampleRate >= 16000)
        Array(channels) { VoiceBiquad.bandPass(sampleRate, 6100.0, 1.4) } else emptyArray()
    private val frame = DoubleArray(channels)
    private val hissFrame = DoubleArray(channels)
    private val compAmount = tone.compression.toDouble() * strength
    private val noiseAmount = tone.noiseReduction.toDouble() * strength
    private val deEssAmount = tone.deEsser.toDouble() * strength
    private val attack = exp(-1.0 / (sampleRate * 0.010))
    private val release = exp(-1.0 / (sampleRate * 0.140))
    private val hissAttack = exp(-1.0 / (sampleRate * 0.004))
    private val hissRelease = exp(-1.0 / (sampleRate * 0.065))
    private val gateSmooth = exp(-1.0 / (sampleRate * 0.018))
    private val threshold = 10.0.pow(-18.0 / 20.0)
    private val ratio = 1.0 + compAmount * 2.7
    // Ganho fixo e limitado para repor volume perdido no compressor, sem tentar normalizar LUFS.
    private val makeup = 10.0.pow((if (tone.autoGain) min(4.2, 0.8 + 5.0 * compAmount) else 0.0) / 20.0)
    private var envelope = 0.0
    private var sibilanceEnvelope = 0.0
    private var gateGain = 1.0

    init { require(sampleRate >= 8000 && channels in 1..8) }

    /** Processa um frame intercalado de 1 a 8 canais sem alocação por amostra. */
    fun process(samples: DoubleArray): DoubleArray {
        require(samples.size == channels)
        var peak = 0.0
        var hissPeak = 0.0
        for (ch in 0 until channels) {
            var value = if (samples[ch].isFinite()) samples[ch].coerceIn(-1.0, 1.0) else 0.0
            value = highPass[ch].apply(value)
            value = lowShelf[ch].apply(value)
            value = bodyPeak[ch].apply(value)
            value = clarityPeak[ch].apply(value)
            value = highShelf[ch].apply(value)
            frame[ch] = value
            peak = max(peak, abs(value))
            if (sibilanceBand.isNotEmpty()) {
                hissFrame[ch] = sibilanceBand[ch].apply(value)
                hissPeak = max(hissPeak, abs(hissFrame[ch]))
            }
        }
        val coefficient = if (peak > envelope) attack else release
        envelope = coefficient * envelope + (1.0 - coefficient) * peak
        val hissCoef = if (hissPeak > sibilanceEnvelope) hissAttack else hissRelease
        sibilanceEnvelope = hissCoef * sibilanceEnvelope + (1.0 - hissCoef) * hissPeak
        val reduction = if (compAmount > 0.0 && envelope > threshold)
            (threshold / envelope).pow(1.0 - 1.0 / ratio) else 1.0
        // Atenuação leve somente em trechos muito baixos. Não remove ruído por IA.
        val gateTarget = if (noiseAmount == 0.0) 1.0 else
            1.0 - noiseAmount * 0.78 * (1.0 - ((envelope - 0.004) / 0.020).coerceIn(0.0, 1.0))
        gateGain = gateSmooth * gateGain + (1.0 - gateSmooth) * gateTarget
        // Sibilância destacada em relação à energia da voz aciona atenuação de banda.
        val hissRatio = if (envelope > 0.015) sibilanceEnvelope / envelope else 0.0
        val deEssGain = (deEssAmount * 0.65 * ((hissRatio - 0.15) / 0.30).coerceIn(0.0, 1.0))
        for (ch in 0 until channels) {
            val v = ((frame[ch] - hissFrame[ch] * deEssGain) * reduction * gateGain * makeup)
            samples[ch] = softLimit(v)
        }
        return samples
    }

    fun reset() {
        highPass.forEach { it.reset() }
        lowShelf.forEach { it.reset() }
        bodyPeak.forEach { it.reset() }
        clarityPeak.forEach { it.reset() }
        highShelf.forEach { it.reset() }
        sibilanceBand.forEach { it.reset() }
        envelope = 0.0
        sibilanceEnvelope = 0.0
        gateGain = 1.0
    }

    /** Evita hard clipping em PCM. Limita picos de amostra, não true peak entre amostras. */
    private fun softLimit(v: Double): Double {
        if (!v.isFinite()) return 0.0
        val x = abs(v)
        val knee = 0.89
        if (x <= knee) return v
        val y = knee + (0.999 - knee) * (1.0 - exp(-(x - knee) / (0.999 - knee)))
        return if (v < 0.0) -y else y
    }
}

/** Coeficientes normalizados RBJ; estado independente por canal. */
private class VoiceBiquad(
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
                               a0: Double, a1: Double, a2: Double): VoiceBiquad =
            VoiceBiquad(b0/a0, b1/a0, b2/a0, a1/a0, a2/a0)
        private fun omega(sr: Int, hz: Double) = 2.0 * PI * hz.coerceAtMost(sr * 0.42) / sr
        fun highPass(sr: Int, hz: Double, q: Double): VoiceBiquad {
            val w = omega(sr,hz); val c = cos(w); val a = sin(w)/(2*q)
            return normalized((1+c)/2,-(1+c),(1+c)/2,1+a,-2*c,1-a)
        }
        fun peak(sr: Int, hz: Double, q: Double, db: Double): VoiceBiquad {
            val w = omega(sr,hz); val c=cos(w); val alpha=sin(w)/(2*q); val a=10.0.pow(db/40)
            return normalized(1+alpha*a,-2*c,1-alpha*a,1+alpha/a,-2*c,1-alpha/a)
        }
        fun lowShelf(sr: Int, hz: Double, db: Double): VoiceBiquad {
            val w = omega(sr,hz); val c=cos(w); val s=sin(w); val a=10.0.pow(db/40)
            val alpha=s/2*sqrt(2.0); val delta=2*sqrt(a)*alpha
            return normalized(a*((a+1)-(a-1)*c+delta),2*a*((a-1)-(a+1)*c),
                a*((a+1)-(a-1)*c-delta),(a+1)+(a-1)*c+delta,
                -2*((a-1)+(a+1)*c),(a+1)+(a-1)*c-delta)
        }
        fun highShelf(sr: Int, hz: Double, db: Double): VoiceBiquad {
            val w = omega(sr,hz); val c=cos(w); val s=sin(w); val a=10.0.pow(db/40)
            val alpha=s/2*sqrt(2.0); val delta=2*sqrt(a)*alpha
            return normalized(a*((a+1)+(a-1)*c+delta),-2*a*((a-1)+(a+1)*c),
                a*((a+1)+(a-1)*c-delta),(a+1)-(a-1)*c+delta,
                2*((a-1)-(a+1)*c),(a+1)-(a-1)*c-delta)
        }
        fun bandPass(sr: Int, hz: Double, q: Double): VoiceBiquad {
            val w = omega(sr,hz); val c=cos(w); val alpha=sin(w)/(2*q)
            return normalized(alpha,0.0,-alpha,1+alpha,-2*c,1-alpha)
        }
    }
}
