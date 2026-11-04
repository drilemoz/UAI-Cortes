package br.com.uaicortes

import java.util.UUID

/** Modelos pessoais de aparência: nunca armazenam o texto nem o intervalo da legenda. */
internal data class SubtitleUserPreset(
    val id: String,
    val name: String,
    val sample: SubtitleCue
) {
    /** Aplica estilo e animação; preserva identificação, conteúdo, tempos e posição. */
    fun applyTo(cue: SubtitleCue): SubtitleCue =
        SubtitleStyles.applySelectedToAll(
            listOf(cue), sample,
            SubtitleStyles.CopyOptions(appearance = true, animation = true, position = false)
        ).single()
}

internal object SubtitleUserPresets {
    const val MAX_ITEMS = 20
    const val MAX_NAME_CHARS = 32

    fun normalizedName(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    fun nameError(name: String, existing: List<SubtitleUserPreset>, excludeId: String? = null): String? {
        val clean = normalizedName(name)
        return when {
            clean.isBlank() -> "Informe um nome para o modelo."
            clean.length > MAX_NAME_CHARS -> "Use até $MAX_NAME_CHARS caracteres no nome."
            clean.any { Character.isISOControl(it) } -> "O nome contém caracteres inválidos."
            existing.any { it.id != excludeId && it.name.equals(clean, ignoreCase = true) } ->
                "Já existe um modelo com esse nome."
            else -> null
        }
    }

    /** Captura somente campos de aparência. x/y e dados por palavra são exclusivos da legenda. */
    fun snapshot(cue: SubtitleCue): SubtitleCue = cue.copy(
        id = "", text = "", startMs = 0L, endMs = 1L,
        x = .5f, y = .92f, wordTimings = emptyList(), wordColors = emptyList()
    )

    fun add(existing: List<SubtitleUserPreset>, name: String, cue: SubtitleCue): List<SubtitleUserPreset> {
        require(existing.size < MAX_ITEMS) { "Limite de $MAX_ITEMS modelos atingido." }
        require(nameError(name, existing) == null) { nameError(name, existing) ?: "Nome inválido." }
        return existing + SubtitleUserPreset(UUID.randomUUID().toString(), normalizedName(name), snapshot(cue))
    }

    fun rename(existing: List<SubtitleUserPreset>, id: String, name: String): List<SubtitleUserPreset> {
        require(existing.any { it.id == id }) { "Modelo não encontrado." }
        require(nameError(name, existing, id) == null) { nameError(name, existing, id) ?: "Nome inválido." }
        return existing.map { if (it.id == id) it.copy(name = normalizedName(name)) else it }
    }

    fun remove(existing: List<SubtitleUserPreset>, id: String): List<SubtitleUserPreset> =
        existing.filterNot { it.id == id }
}
