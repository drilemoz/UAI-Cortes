package br.com.uaicortes

/** Um único play por seleção: chegar ao fim nunca reinicia o clipe automaticamente. */
internal object TrimPreviewEndPolicy {
    fun reachedEnd(positionMs: Long, endMs: Long, playbackEnded: Boolean): Boolean =
        playbackEnded || (endMs > 0L && positionMs >= endMs)
}
