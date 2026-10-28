package br.com.uaicortes

import kotlin.math.pow

/** Efeitos possíveis em sequências Media3 sem sobrepor faixas de vídeo.
 * A cor de cada frame muda apenas perto das junções; o tempo total não muda.
 * Não há dissolvência ou movimento do frame seguinte sobre o anterior.
 */
object TransitionEffects {
    const val CUT = "Corte direto"
    const val BLACK = "Fade preto"
    const val WHITE = "Fade branco"
    const val FLASH_WHITE = "Flash branco"
    const val FLASH_BLACK = "Flash preto"
    const val BLUE = "Fade azul"
    const val GOLD = "Fade dourado"
    const val MONO = "Preto e branco"
    const val SEPIA = "Sépia"
    const val ZOOM_IN = "Zoom cinematográfico"
    const val ZOOM_OUT = "Recuo suave"
    const val PAN = "Deslocamento lateral"

    val names = listOf(CUT, ZOOM_IN, ZOOM_OUT, PAN, BLACK, WHITE, FLASH_WHITE, FLASH_BLACK, BLUE, GOLD, MONO, SEPIA)

    fun supported(kind: String): Boolean = kind in names
    fun isMotion(kind: String) = kind == ZOOM_IN || kind == ZOOM_OUT || kind == PAN
    fun motionScale(kind: String, strength: Float): Float {
        val s = strength.coerceIn(0f, 1f)
        return when (kind) {
            ZOOM_IN -> 1f + 0.13f * s
            ZOOM_OUT -> 1f + 0.075f * s
            PAN -> 1f + 0.09f * s
            else -> 1f
        }
    }
    fun motionX(kind: String, strength: Float): Float =
        if (kind == PAN) 0.06f * strength.coerceIn(0f, 1f) else 0f

    fun description(kind: String): String = when (kind) {
        CUT -> "Troca imediata de um vídeo para o próximo, sem efeito."
        BLACK -> "Escurece até preto na junção e clareia no clipe seguinte."
        WHITE -> "Clareia até branco na junção e volta à imagem seguinte."
        FLASH_WHITE -> "Clarão branco rápido no ponto de troca."
        FLASH_BLACK -> "Pulso escuro rápido no ponto de troca."
        BLUE -> "Banho azul na saída e na entrada dos clipes."
        GOLD -> "Banho dourado na saída e na entrada dos clipes."
        MONO -> "A imagem perde a cor na junção e recupera a cor depois."
        SEPIA -> "A imagem fica sépia na junção e retoma a cor natural depois."
        ZOOM_IN -> "Aproximação de câmera suave junto à troca, sem ampliar a duração."
        ZOOM_OUT -> "Recuo cinematográfico na saída e entrada dos vídeos."
        PAN -> "Deslocamento lateral discreto, preservando as bordas do quadro."
        else -> "Corte direto."
    }

    /** Força da transição em um único lado da junção, de 0 (normal) a 1 (pico).
     * A animação ocorre em um clipe por vez, sem sobreposição temporal.
     */
    fun intensity(kind: String, sideProgress: Float): Float {
        if (!supported(kind) || kind == CUT) return 0f
        val p = sideProgress.coerceIn(0f, 1f)
        return when (kind) {
            FLASH_WHITE, FLASH_BLACK -> p.pow(5)
            else -> p
        }
    }

    /** Matriz 4x4 column-major, como exige androidx.media3.effect.RgbMatrix.
     * Multiplica RGBA de entrada em [0..1], sem alterar o canal alfa.
     */
    fun matrix(kind: String, strength: Float): FloatArray {
        val s = strength.coerceIn(0f, 1f)
        val base = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
        val target = when (kind) {
            MONO -> floatArrayOf(
                .2126f,.7152f,.0722f,
                .2126f,.7152f,.0722f,
                .2126f,.7152f,.0722f
            )
            SEPIA -> floatArrayOf(
                .393f,.769f,.189f,
                .349f,.686f,.168f,
                .272f,.534f,.131f
            )
            BLACK, WHITE, FLASH_WHITE, FLASH_BLACK, BLUE, GOLD -> FloatArray(9)
            else -> base
        }
        val blend = FloatArray(9) { i -> base[i] * (1f - s) + target[i] * s }
        val bias = when (kind) {
            WHITE, FLASH_WHITE -> floatArrayOf(s,s,s)
            BLUE -> floatArrayOf(.08f*s,.28f*s,.95f*s)
            GOLD -> floatArrayOf(1f*s,.68f*s,.18f*s)
            else -> floatArrayOf(0f,0f,0f)
        }
        return floatArrayOf(
            blend[0],blend[3],blend[6],0f,
            blend[1],blend[4],blend[7],0f,
            blend[2],blend[5],blend[8],0f,
            bias[0],bias[1],bias[2],1f
        )
    }

    /** Escolhe qual junção está atuando no frame. Válido para trechos curtos
     * com transições nos dois extremos: a mais forte prevalece.
     */
    fun matrixAt(
        localMs: Long, clipMs: Long,
        incomingKind: String, incomingMs: Long,
        outgoingKind: String, outgoingMs: Long
    ): FloatArray {
        val t = localMs.coerceIn(0L, clipMs.coerceAtLeast(0L))
        val incoming = if (incomingMs > 0L) intensity(incomingKind,
            ((incomingMs - t).toFloat() / incomingMs).coerceIn(0f,1f)) else 0f
        val outgoing = if (outgoingMs > 0L) intensity(outgoingKind,
            ((t - (clipMs - outgoingMs)).toFloat() / outgoingMs).coerceIn(0f,1f)) else 0f
        return if (incoming >= outgoing) matrix(incomingKind, incoming)
            else matrix(outgoingKind, outgoing)
    }

    /** Adapta a matriz normalizada de Media3 à ColorMatrix Compose (bias em 255). */
    fun composeValues(matrix: FloatArray): FloatArray = floatArrayOf(
        matrix[0],matrix[4],matrix[8],0f,matrix[12]*255f,
        matrix[1],matrix[5],matrix[9],0f,matrix[13]*255f,
        matrix[2],matrix[6],matrix[10],0f,matrix[14]*255f,
        0f,0f,0f,1f,0f
    )
}
