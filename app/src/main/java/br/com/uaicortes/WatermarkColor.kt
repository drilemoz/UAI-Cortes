package br.com.uaicortes

import java.util.Locale

/** A opacidade é ajustada separadamente; o hexadecimal aceita apenas RGB. */
internal object WatermarkColor {
    fun hex(argb: Int): String = String.format(Locale.ROOT, "%06X", argb and 0xFFFFFF)
    fun parse(text: String): Int? {
        val rgb = text.trim().removePrefix("#")
        if (rgb.length != 6 || rgb.any { it.digitToIntOrNull(16) == null }) return null
        return rgb.toIntOrNull(16)?.or(0xFF000000.toInt())
    }
}
