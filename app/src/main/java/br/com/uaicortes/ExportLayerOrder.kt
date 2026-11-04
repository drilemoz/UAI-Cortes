package br.com.uaicortes

/** Na prévia Compose a última camada da lista fica por cima.
 * Media3 DefaultVideoCompositor desenha a primeira entrada de vídeo por cima.
 * A conversão é APENAS para a ordem de composição; não reordena o projeto.
 */
internal object ExportLayerOrder {
    fun <T> media3TopFirst(pipsInEditorOrder: List<T>): List<T> =
        pipsInEditorOrder.asReversed()
}
