package br.com.uaicortes

import kotlin.math.abs

/** Solução determinística de layout. Os itens só colidem quando seus tempos se cruzam. */
data class PlacementItem(
    val id: String, val startMs: Long, val durationMs: Long,
    val x: Float, val y: Float, val width: Float, val height: Float
)

object OverlayPlacement {
    const val MAX_PIPS = 4
    private const val GAP = .012f

    fun intersects(a: PlacementItem, b: PlacementItem, ax: Float = a.x, ay: Float = a.y,
                   bx: Float = b.x, by: Float = b.y): Boolean {
        val overlapTime = a.startMs < b.startMs + b.durationMs && b.startMs < a.startMs + a.durationMs
        val overlapX = abs(ax - bx) < (a.width + b.width) / 2f + GAP
        val overlapY = abs(ay - by) < (a.height + b.height) / 2f + GAP
        return overlapTime && overlapX && overlapY
    }

    /** O item editado tem prioridade, mas pode ceder um pouco de espaço quando
     * não existir distribuição válida na posição solicitada. Nunca altera a escala. */
    fun arrange(items: List<PlacementItem>, preferredId: String? = null): Map<String, Pair<Float, Float>>? {
        if (items.size > MAX_PIPS || items.map { it.id }.distinct().size != items.size) return null
        if (items.any { it.width <= 0f || it.height <= 0f || it.width > .95f || it.height > .95f }) return null
        if (items.isEmpty()) return emptyMap()
        val ordered = items.sortedWith(compareByDescending<PlacementItem> { it.id == preferredId }
            .thenBy { items.indexOf(it) })
        val first = ordered.first()
        val original = OverlayEditorLogic.clampCenter(first.x, first.width) to
            OverlayEditorLogic.clampCenter(first.y, first.height)
        // Primeiro tenta manter o PiP tocado onde o usuário o colocou. Só se não
        // for possível, avalia posições adjacentes para TODAS as camadas.
        val anchors = buildList {
            add(original)
            for (r in 0..2) for (c in 0..2) {
                val candidate = OverlayEditorLogic.preset(r, c, first.width, first.height)
                if (candidate != original) add(candidate)
            }
        }.sortedWith(compareBy<Pair<Float, Float>> {
            (it.first - original.first) * (it.first - original.first) +
                (it.second - original.second) * (it.second - original.second)
        }.thenBy { it.second }.thenBy { it.first })
        // Plano B: preencher os quatro cantos antes dos pontos intermediários.
        // Evita falsos "sem espaço" quando um posicionamento guloso ocupa o centro.
        for (cornerFirst in listOf(false, true)) for (anchor in anchors) {
            val positions = linkedMapOf(first.id to anchor)
            val placed = mutableListOf(first)
            var failed = false
            for (item in ordered.drop(1)) {
                val wanted = OverlayEditorLogic.clampCenter(item.x, item.width) to
                    OverlayEditorLogic.clampCenter(item.y, item.height)
                val candidates = mutableListOf(wanted)
                for (r in 0..20) for (c in 0..20) {
                    candidates += OverlayEditorLogic.clampCenter(c / 20f, item.width) to
                        OverlayEditorLogic.clampCenter(r / 20f, item.height)
                }
                val corners = listOf(
                    OverlayEditorLogic.preset(0, 0, item.width, item.height),
                    OverlayEditorLogic.preset(0, 2, item.width, item.height),
                    OverlayEditorLogic.preset(2, 0, item.width, item.height),
                    OverlayEditorLogic.preset(2, 2, item.width, item.height))
                val sorted = candidates.distinct().sortedWith(compareBy<Pair<Float, Float>> {
                    if (cornerFirst && it !in corners) 1 else 0
                }.thenBy {
                    (it.first - wanted.first) * (it.first - wanted.first) +
                        (it.second - wanted.second) * (it.second - wanted.second)
                }.thenBy { it.second }.thenBy { it.first })
                val fit = sorted.firstOrNull { (x, y) ->
                    placed.none { other ->
                        val pos = positions.getValue(other.id)
                        intersects(item, other, x, y, pos.first, pos.second)
                    }
                }
                if (fit == null) { failed = true; break }
                positions[item.id] = fit
                placed += item
            }
            if (!failed) return positions
        }
        return null
    }
}
