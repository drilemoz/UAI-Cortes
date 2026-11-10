package br.com.uaicortes

/** Diagnóstico estático, sem editar ou apagar legendas do projeto. */
internal object ExportSubtitlePolicy {
    data class Summary(val valid: Int, val overlaps: Int, val outside: Int, val invalid: Int) {
        val warnings: List<String> get() = buildList {
            if (overlaps > 0) add("$overlaps sobreposição(ões): somente a primeira legenda ativa será desenhada.")
            if (outside > 0) add("$outside legenda(s) ultrapassam o fim do vídeo e serão cortadas.")
            if (invalid > 0) add("$invalid legenda(s) têm tempo inválido e não aparecerão.")
        }
    }

    fun summarize(cues: List<SubtitleCue>, durationMs: Long): Summary {
        // Uma cue começada antes de 0 ms pode continuar visível no primeiro quadro.
        // Visibilidade temporal: interseção não vazia entre [início,fim) e [0,duração).
        val valid = cues.filter { it.text.isNotBlank() && durationMs > 0L &&
            it.endMs > it.startMs && it.endMs > 0L && it.startMs < durationMs }
        val invalid = cues.count { it.text.isNotBlank() && it !in valid }
        val outside = valid.count { it.endMs > durationMs }
        // Varredura O(n log n), mesmo em projetos com milhares de legendas.
        // Conta pares conflitantes, sem tentar adivinhar quais palavras foram ocultadas.
        val ends = java.util.PriorityQueue<Long>()
        var overlaps = 0
        valid.sortedBy { it.startMs }.forEach { cue ->
            while (ends.isNotEmpty() && ends.peek() <= cue.startMs.coerceAtLeast(0L)) ends.remove()
            overlaps += ends.size
            ends.add(cue.endMs)
        }
        return Summary(valid.size, overlaps, outside, invalid)
    }
}
