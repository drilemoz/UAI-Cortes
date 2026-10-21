package br.com.uaicortes

/** Indexação circular sem cópias dos elementos em memória.
 * A LazyRow cria só os itens visíveis, mesmo com a janela virtual muito longa.
 */
object CircularCarousel {
    fun startIndex(count: Int): Int {
        require(count > 0) { "Carrossel vazio" }
        val middle = Int.MAX_VALUE / 2
        return middle - middle % count
    }

    fun optionIndex(absoluteIndex: Int, count: Int): Int {
        require(count > 0) { "Carrossel vazio" }
        require(absoluteIndex >= 0) { "Posição negativa" }
        return absoluteIndex % count
    }
}
