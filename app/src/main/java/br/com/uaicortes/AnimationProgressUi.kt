package br.com.uaicortes

/** Percentual do exportador para uma captura estável da barra Compose. */
internal object AnimationProgressUi {
    fun fraction(value:Int?):Float?=value?.coerceIn(0,100)?.div(100f)
    fun title(stage:String):String=when(stage) {
        "Preparando imagens" -> "Preparando imagens"
        "Exportando vídeo" -> "Uai Cortes · Exportando vídeo"
        "Salvando na galeria" -> "Salvando na galeria"
        else -> "Uai Cortes · Processando"
    }
}
