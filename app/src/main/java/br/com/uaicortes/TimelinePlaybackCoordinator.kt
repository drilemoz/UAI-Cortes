package br.com.uaicortes

/** Destino de um seek na timeline do projeto, em coordenadas do clipe recortado. */
internal data class TimelineSeekTarget(val clipIndex: Int, val localMs: Long)

/**
 * Converte posições entre o projeto e os itens concatenados do Media3.
 * Não depende de Activity, ExoPlayer ou Compose, podendo ser testado em JVM pura.
 * O limite exato entre dois clipes pertence ao clipe anterior, por compatibilidade
 * com a linha do tempo existente. Não modifica os tempos de origem (startMs/endMs).
 */
internal object TimelinePlaybackCoordinator {
    private fun addSaturated(a: Long, b: Long): Long =
        if (b > Long.MAX_VALUE - a) Long.MAX_VALUE else a + b

    fun total(durationsMs: List<Long>): Long = durationsMs.fold(0L) { sum, duration ->
        addSaturated(sum, duration.coerceAtLeast(0L))
    }

    fun target(durationsMs: List<Long>, projectMs: Long): TimelineSeekTarget? {
        if (durationsMs.isEmpty()) return null
        var remaining = projectMs.coerceIn(0L, total(durationsMs))
        durationsMs.forEachIndexed { index, rawDuration ->
            val duration = rawDuration.coerceAtLeast(0L)
            if (remaining <= duration || index == durationsMs.lastIndex) {
                return TimelineSeekTarget(index, remaining.coerceIn(0L, duration))
            }
            remaining -= duration
        }
        return null
    }

    fun projectPosition(durationsMs: List<Long>, clipIndex: Int, localMs: Long): Long {
        if (durationsMs.isEmpty()) return 0L
        val index = clipIndex.coerceIn(0, durationsMs.lastIndex)
        val prior = total(durationsMs.take(index))
        val within = localMs.coerceIn(0L, durationsMs[index].coerceAtLeast(0L))
        return addSaturated(prior, within).coerceAtMost(total(durationsMs))
    }

    fun step(durationsMs: List<Long>, projectMs: Long, deltaMs: Long): TimelineSeekTarget? =
        target(durationsMs, TimelinePrecision.step(projectMs, deltaMs, total(durationsMs)))
}
