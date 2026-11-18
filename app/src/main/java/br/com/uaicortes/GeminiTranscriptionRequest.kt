package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

internal object GeminiTranscriptionRequest {
    const val MODEL = "gemini-3.5-transcribe"
    const val LABEL = "Gemini 3.5 Transcribe"
    const val INLINE_MAX_BYTES = 2 * 1024 * 1024
    const val PROBE_MAX_BYTES = 128 * 1024
    const val PROBE_DURATION_MS = 4_000L

    // Escolha feita ANTES de qualquer rede. Erro no envio direto não dispara outro pedido.
    fun useInline(audioBytes: Long) = audioBytes in 1L..INLINE_MAX_BYTES.toLong()

    fun inline(audio: ByteArray): JSONObject {
        require(useInline(audio.size.toLong()))
        return JSONObject().put("type", "audio").put("mime_type", "audio/wav")
            .put("data", Base64.getEncoder().encodeToString(audio))
    }

    fun file(uri: String): JSONObject = JSONObject().put("type", "file").put("file_uri", uri)

    fun body(input: JSONObject, speakers: Boolean, sample: Boolean = false): JSONObject = JSONObject()
        .put("model", "models/$MODEL")
        .put("input", JSONArray().put(input))
        .put("generation_config", JSONObject().put("transcription_config",
            JSONObject().put("language_codes", JSONArray().put(if (sample) "en-US" else "pt-BR"))
                .put("mode", JSONObject().put("type", "verbatim")
                    .put("timestamp_granularities", JSONArray().put("word"))
                    .apply { if (speakers) put("diarization_mode", "speaker") })))
}
