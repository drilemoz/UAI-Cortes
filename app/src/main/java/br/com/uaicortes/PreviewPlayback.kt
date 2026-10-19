package br.com.uaicortes

/** Velocidades suportadas exclusivamente para visualização; nunca são exportadas. */
object PreviewPlayback {
    val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    fun label(speed: Float): String {
        require(SPEEDS.contains(speed)) { "Velocidade não suportada: $speed" }
        val value = speed.toString().removeSuffix(".0").replace('.', ',')
        return "${value}×"
    }
}
