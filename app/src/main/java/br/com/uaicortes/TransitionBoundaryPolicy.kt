package br.com.uaicortes

/** Existem N-1 junções para N clipes; o índice da primeira junção é zero. */
internal object TransitionBoundaryPolicy {
    fun count(clipCount: Int): Int = (clipCount - 1).coerceAtLeast(0)
    fun clamp(index: Int, clipCount: Int): Int = index.coerceIn(0, (clipCount - 2).coerceAtLeast(0))
    fun isValid(index: Int, clipCount: Int): Boolean = index >= 0 && index < count(clipCount)
}
