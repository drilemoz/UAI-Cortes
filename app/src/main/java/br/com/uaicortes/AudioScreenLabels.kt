package br.com.uaicortes

/** Texto para resumo, mantendo intacto o título persistido e o URI original. */
internal object AudioScreenLabels {
    fun activeTrackCount(clips:Int,tracks:Int,legacyMusic:Boolean):Int =
        clips.coerceAtLeast(0) + tracks.coerceAtLeast(0) + if(legacyMusic) 1 else 0

    /** Identificadores automáticos longos não são títulos úteis na mixagem. */
    fun readableVideoClip(raw:String, index:Int):String {
        val file = raw.substringAfterLast('/').trim()
        val stem = file.substringBeforeLast('.', file)
        val automaticId = stem.matches(Regex("[0-9]{8,}(_[0-9]{8,})*"))
        val label = if (file.isBlank() || automaticId) "Áudio original" else file
        return "Vídeo ${index+1} · $label"
    }

    fun readableFileName(raw:String):String {
        val file=raw.substringAfterLast('/').substringBeforeLast('.').replace('_',' ').trim()
        return file.ifBlank { "Faixa de áudio" }
    }
}
