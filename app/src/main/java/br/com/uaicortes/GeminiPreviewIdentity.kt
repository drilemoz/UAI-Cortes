package br.com.uaicortes

import java.security.MessageDigest

internal object GeminiPreviewIdentity {
    const val TEXT = "Olá! Esta é uma amostra da minha voz."
    fun id(apiKey: String, model: String, voiceId: String, styleInstruction: String): String {
        val input = listOf("gemini-sample-v1", apiKey, model, voiceId, styleInstruction, TEXT)
        val md = MessageDigest.getInstance("SHA-256")
        input.forEach { item ->
            val bytes = item.toByteArray(Charsets.UTF_8)
            md.update(byteArrayOf((bytes.size ushr 24).toByte(), (bytes.size ushr 16).toByte(),
                (bytes.size ushr 8).toByte(), bytes.size.toByte()))
            md.update(bytes)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

