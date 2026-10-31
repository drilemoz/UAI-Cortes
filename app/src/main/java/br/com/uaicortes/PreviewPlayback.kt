package br.com.uaicortes

/** Velocidades apenas da prévia. Não alteram o vídeo exportado nem a duração do projeto. */
object PreviewPlayback {
    val SPEEDS = listOf(0.1f, 0.2f, 0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f,
        2f, 2.5f, 3f, 4f, 5f, 6f)
    fun label(speed: Float): String {
        require(SPEEDS.contains(speed)) { "Velocidade não suportada: $speed" }
        val value = speed.toString().removeSuffix(".0").replace('.', ',')
        return "${value}×"
    }
    fun previous(speed: Float): Float {
        val index = SPEEDS.indexOf(speed).takeIf { it >= 0 } ?: SPEEDS.indexOf(1f)
        return SPEEDS[(index - 1).coerceAtLeast(0)]
    }
    fun next(speed: Float): Float {
        val index = SPEEDS.indexOf(speed).takeIf { it >= 0 } ?: SPEEDS.indexOf(1f)
        return SPEEDS[(index + 1).coerceAtMost(SPEEDS.lastIndex)]
    }
}
