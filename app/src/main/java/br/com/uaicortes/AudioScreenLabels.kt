package br.com.uaicortes

/** Texto para resumo, mantendo intacto o título persistido e o URI original. */
internal object AudioScreenLabels {
    fun activeTrackCount(clips:Int,tracks:Int,legacyMusic:Boolean):Int =
        clips.coerceAtLeast(0) + tracks.coerceAtLeast(0) + if(legacyMusic) 1 else 0

    fun readableFileName(raw:String):String {
        val file=raw.substringAfterLast('/').substringBeforeLast('.').replace('_',' ').trim()
        return file.ifBlank { "Faixa de áudio" }
    }
}
