package br.com.uaicortes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Escala de formas para o editor: simples, compacta e igual nos três temas. */
internal object UaiShapes {
    val chip = RoundedCornerShape(6.dp)
    val button = RoundedCornerShape(8.dp)
    val item = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(10.dp)
    val dialog = RoundedCornerShape(12.dp)
    val sheet = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    val theme = Shapes(
        extraSmall = chip,
        small = button,
        medium = card,
        large = dialog,
        extraLarge = dialog
    )
}
