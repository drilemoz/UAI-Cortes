package br.com.uaicortes

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/** Each successful WAV is committed independently, before requesting the next scene. */
internal class VoiceStudioBatch(root: File, identity: String) {
    private val directory = File(root, "voice_studio_parts/${fingerprint(identity)}")
    private fun target(index: Int) = File(directory, "${index}.wav")
    fun existing(index: Int): Pair<File, Long>? = runCatching {
        val file = target(index)
        if (!file.isFile || file.length() !in 45L..MAX_BYTES.toLong()) return null
        file to ((pcm(file.readBytes()).samplesAtOutputRate().toLong() * 1000 + RATE - 1) / RATE)
    }.getOrNull()

    fun save(index: Int, wave: GeminiWave) {
        require(GeminiWaveParser.duration(wave.bytes, "audio/wav") == wave.durationMs)
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Não foi possível guardar os trechos.")
        val file = target(index)
        val part = File.createTempFile("wave-", ".part", directory)
        try {
            part.outputStream().use { it.write(wave.bytes); it.fd.sync() }
            if (!part.renameTo(file)) throw IOException("Não foi possível concluir o trecho.")
        } finally { part.delete() }
    }

    fun completed(scenes: List<VoiceStudioPlan.Scene>): List<Pair<File, Long>?> =
        scenes.indices.map(::existing)

    /** Fill gaps with PCM silence, never trim speech or shift the following scene. */
    fun assemble(scenes: List<VoiceStudioPlan.Scene>, ready: List<Pair<File, Long>?>): GeminiWave {
        require(scenes.isNotEmpty() && ready.size == scenes.size && ready.all { it != null })
        val last = scenes.last()
        val maxDuration = MAX_BYTES.toLong() * 1000 / (2 * RATE)
        require(last.startMs in 0 until maxDuration) { "A narração é longa demais para um único WAV." }
        val endMs = last.startMs + ready.last()!!.second
        require(endMs in 1 until maxDuration) { "A narração é longa demais para um único WAV." }
        val lastPcm = pcm(ready.last()!!.first.readBytes())
        val lastSamples = samplesAt(last.startMs) + lastPcm.samplesAtOutputRate()
        val size = lastSamples * 2
        require(size <= MAX_BYTES - 44) { "A narração é longa demais para um único WAV." }
        val bytes = ByteArray(size + 44)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + size)
            put("WAVEfmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(RATE); putInt(2 * RATE); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(size)
        }
        for (i in scenes.indices) {
            val scene = scenes[i]
            val audio = if (i == scenes.lastIndex) lastPcm else pcm(ready[i]!!.first.readBytes())
            val offset = samplesAt(scene.startMs)
            val length = audio.samplesAtOutputRate()
            require(length <= samplesAt(scene.windowMs)) { "A fala da cena ${i + 1} passa do tempo disponível." }
            require(offset + length <= lastSamples)
            if (audio.rate == RATE) {
                audio.raw.copyInto(bytes, 44 + offset * 2, audio.start, audio.start + length * 2)
            } else {
                for (j in 0 until length) {
                    val rawIndex = minOf(j.toLong() * audio.rate / RATE, (audio.size / 2L) - 1).toInt()
                    val at = audio.start + rawIndex * 2
                    bytes[44 + (offset + j) * 2] = audio.raw[at]
                    bytes[45 + (offset + j) * 2] = audio.raw[at + 1]
                }
            }
        }
        return GeminiWave(bytes, GeminiWaveParser.duration(bytes, "audio/wav"))
    }

    /** A batch key includes the frozen plan/settings. No key or text is stored in a pathname. */
    companion object {
        private const val RATE = 24000
        private const val MAX_BYTES = 32 * 1024 * 1024
        private fun samplesAt(ms: Long): Int = (ms * RATE / 1000).toInt()
        private fun fingerprint(text: String): String = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
        fun identity(text: String, model: String, voice: String, style: String, language: String,
            scenes: List<VoiceStudioPlan.Scene>): String = buildString {
            append("1|")
            listOf(text, model, voice, style, language).forEach { append(it.length).append(':').append(it) }
            scenes.forEach { append('|').append(it.windowMs).append(':').append(it.text.length).append(':').append(it.text) }
        }
    }

    private data class Pcm(val raw: ByteArray, val start: Int, val size: Int, val rate: Int) {
        fun samplesAtOutputRate(): Int = ((size / 2L * RATE + rate - 1) / rate).toInt()
    }
    private fun pcm(raw: ByteArray): Pcm {
        GeminiWaveParser.duration(raw, "audio/wav")
        val reader = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        var at = 12
        var rate = 0
        var start = 0
        var size = 0
        while (at + 8 <= raw.size) {
            val length = reader.getInt(at + 4)
            when (String(raw, at, 4, Charsets.US_ASCII)) {
                "fmt " -> rate = reader.getInt(at + 12)
                "data" -> { start = at + 8; size = length }
            }
            at += 8 + length + (length and 1)
        }
        return Pcm(raw, start, size, rate)
    }
}
