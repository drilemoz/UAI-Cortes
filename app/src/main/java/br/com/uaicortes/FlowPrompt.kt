package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject

data class FlowPromptDraft(
    val context: String = "",
    val scenes: List<String> = listOf(""),
    val mode: String = "create"
) {
    fun normalized() = copy(
        context = context.take(1200),
        scenes = scenes.take(12).map { it.take(1800) }.ifEmpty { listOf("") },
        mode = if (mode == "transform") "transform" else "create"
    )
    fun json(): JSONObject = normalized().let { d -> JSONObject()
        .put("context", d.context)
        .put("scenes", JSONArray().also { array -> d.scenes.forEach { array.put(it) } })
        .put("mode", d.mode)
    }
    companion object {
        fun read(j: JSONObject?): FlowPromptDraft? = j?.let {
            val arr = it.optJSONArray("scenes")
            FlowPromptDraft(it.optString("context"),
                if (arr == null) listOf("") else (0 until arr.length()).map { i -> arr.optString(i) },
                it.optString("mode", "create")).normalized()
        }
    }
}

internal object FlowPromptPolicy {
    const val DEFAULT_STYLE = "Animação 3D fotorrealista cinematográfica. Personagens tridimensionais detalhados, movimentos fluidos, expressões faciais expressivas, iluminação realista, texturas detalhadas e composição cinematográfica. Manter a aparência dos personagens, roupas e cenário consistentes nas cenas. Humor visual transmitido por gestos e efeitos sonoros sincronizados. Qualquer texto visível deve estar em português brasileiro. Sem logotipos, marcas d'água ou assinatura visual."
    const val MAX_STYLE_LENGTH = 2000
    fun normalizeStyle(value: String) = value.trim().take(MAX_STYLE_LENGTH).ifBlank { DEFAULT_STYLE }
    fun compose(style: String, draft: FlowPromptDraft, index: Int): String {
        val safe = draft.normalized()
        require(index in safe.scenes.indices) { "Cena inexistente" }
        val scene = safe.scenes[index].trim()
        require(scene.isNotBlank()) { "Descreva a cena antes de copiar." }
        val modeInstruction = if (safe.mode == "transform")
            "Vídeo de referência: preservar movimentos, gestos, sequência das ações e enquadramento da câmera original, alterando apenas o estilo solicitado."
        else "Gerar a ação descrita a partir de texto; não presumir vídeo de referência."
        return buildString {
            append(normalizeStyle(style))
            append("\n\n")
            append(modeInstruction)
            if (safe.context.isNotBlank()) {
                append("\nPersonagens, cenário e continuidade: ")
                append(safe.context.trim())
            }
            append("\nCena "); append(index + 1); append(": "); append(scene)
            append("\nPreservar a continuidade de personagens e cenário. Esta é uma cena independente.")
        }
    }
}
