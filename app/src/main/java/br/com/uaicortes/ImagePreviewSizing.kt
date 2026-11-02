package br.com.uaicortes

import kotlin.math.max
import kotlin.math.roundToInt

/** A qualidade abaixo modifica SOMENTE a visualização; o MP4 mantém suas opções. */
internal enum class ImagePreviewQuality(val label: String, val maxLongSide: Int) {
    FAST("Rápida", 720), BALANCED("Equilibrada", 1280), HIGH("Alta", 1920)
}

/** Dimensões de destino proporcionais, sem ampliar artificialmente fotos pequenas. */
internal object ImagePreviewSizing {
    fun target(width: Int, height: Int, maxLongSide: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxLongSide > 0)
        val divisor = (max(width, height).toFloat() / maxLongSide).coerceAtLeast(1f)
        return (width / divisor).roundToInt().coerceAtLeast(1) to
            (height / divisor).roundToInt().coerceAtLeast(1)
    }
    fun sampleSize(width: Int, height: Int, maxLongSide: Int): Int {
        require(width > 0 && height > 0 && maxLongSide > 0)
        var sample = 1
        while (max(width, height) / (sample * 2L) >= maxLongSide) sample *= 2
        return sample
    }
}

