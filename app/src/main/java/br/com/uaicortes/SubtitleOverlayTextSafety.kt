package br.com.uaicortes

/** Media3 TextOverlay cria uma Bitmap com a largura do texto recebido.
 * Uma string vazia (inclusive no início de "Letra por letra" ou entre legendas)
 * pode produzir uma Bitmap 0px, interrompendo o Transformer.
 *
 * Enviamos uma pequena área de texto transparente em vez disso; OverlaySettings
 * também deixa o overlay completamente invisível quando não há texto a mostrar.
 * Nenhuma legenda salva ou importada é alterada.
 */
object SubtitleOverlayTextSafety {
    private const val INVISIBLE_GLYPH = "\u00A0" // NBSP tem largura positiva no Paint padrão.

    fun isVisible(text: String): Boolean = text.isNotBlank()

    fun bitmapSafeText(text: String): String = if (isVisible(text)) text else INVISIBLE_GLYPH
}
