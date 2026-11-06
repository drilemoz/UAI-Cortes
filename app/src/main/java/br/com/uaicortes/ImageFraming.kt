package br.com.uaicortes

import kotlin.math.max

/** Modelo de enquadramento usado igualmente na prévia e no Media3.
 * As coordenadas de deslocamento são frações dos meios-eixos do quadro final.
 * Nenhuma transformação altera pixels do arquivo original.
 */
internal object ImageFraming {
    const val FIT = "fit"
    const val FILL = "fill"
    const val MAX_ZOOM = 3f

    fun mode(value: String?): String = if (value == FILL) FILL else FIT
    fun zoom(value: Float): Float = if (value.isFinite()) value.coerceIn(1f, MAX_ZOOM) else 1f
    fun pan(value: Float): Float = if (value.isFinite()) value.coerceIn(-1f, 1f) else 0f

    data class Transform(
        val scale: Float, val offsetX: Float, val offsetY: Float,
        val availableX: Float, val availableY: Float
    )

    /** Enquadra uma imagem por inteiro ou com preenchimento, sem distorção.
     * A apresentação inicial do PlayerView/Media3 é FIT; ampliamos depois.
     * Os limites do deslocamento impedem revelar novas bordas vazias no FILL.
     */
    fun resolve(frameMode: String?, extraZoom: Float, horizontalPan: Float, verticalPan: Float,
                sourceAspect: Float, canvasAspect: Float): Transform {
        val src = if (sourceAspect.isFinite() && sourceAspect > .05f) sourceAspect else 1f
        val canvas = if (canvasAspect.isFinite() && canvasAspect > .05f) canvasAspect else 1f
        val relative = src / canvas
        val baseFill = max(relative, 1f / relative)
        val scale = (if (mode(frameMode) == FILL) baseFill else 1f) * zoom(extraZoom)
        val sourceWidth = minOf(1f, relative)
        val sourceHeight = minOf(1f, 1f / relative)
        val roomX = (sourceWidth * scale - 1f).coerceAtLeast(0f)
        val roomY = (sourceHeight * scale - 1f).coerceAtLeast(0f)
        return Transform(scale, pan(horizontalPan) * roomX, pan(verticalPan) * roomY,
            roomX, roomY)
    }

    fun dragPan(oldPan: Float, pixelDelta: Float, axisPixels: Int, available: Float): Float =
        if (axisPixels <= 0 || available <= .0001f) pan(oldPan)
        else pan(oldPan + (pixelDelta * 2f / axisPixels) / available)
}
