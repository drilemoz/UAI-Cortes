package br.com.uaicortes

/** IDs estáveis no rascunho; rótulos podem mudar sem afetar a exportação. */
internal object ImageMotionUi {
    val choices = listOf("auto", "zoom_in", "zoom_out", "horizontal", "vertical")
    fun label(id: String): String = when(id) {
        "zoom_in" -> "Aproximar"
        "zoom_out" -> "Afastar"
        "horizontal" -> "Panorâmica horizontal"
        "vertical" -> "Panorâmica vertical"
        else -> "Movimento automático"
    }
    fun description(id: String): String = when(id) {
        "zoom_in" -> "A câmera se aproxima gradualmente de cada foto."
        "zoom_out" -> "A câmera se afasta gradualmente de cada foto."
        "horizontal" -> "Deslocamento lateral suave, alternando a direção entre fotos."
        "vertical" -> "Deslocamento vertical suave, alternando a direção entre fotos."
        else -> "Alterna movimentos de zoom e deslocamento entre as fotos."
    }
}
