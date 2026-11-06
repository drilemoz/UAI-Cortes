package br.com.uaicortes

import java.security.MessageDigest

/** Identificador de uma amostra de voz: a chave de API autoriza a requisicao, mas nao
 * altera seu conteudo. Reusar amostra com outra chave nao solicita outra geracao.
 * Modelo, voz, estilo e frase fixa continuam separados na identidade SHA-256.
 */
internal object GeminiPreviewIdentity {
    const val TEXT = "Olá! Este é o Uai Cortes. Aqui você corta vídeos, edita cenas e cria novas histórias. Fácil, não é? Agora... vamos ouvir esta voz!"
    @Suppress("UNUSED_PARAMETER")
    fun id(apiKey: String, model: String, voiceId: String, styleInstruction: String): String {
        val input = listOf("gemini-sample-v2", model, voiceId, styleInstruction, TEXT)
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

