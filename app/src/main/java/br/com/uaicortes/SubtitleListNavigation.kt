package br.com.uaicortes

import java.io.Reader
import java.text.Normalizer
import java.util.Locale

/** Navegação e busca da lista compacta, sem dependência de Compose/Android. */
internal object SubtitleListNavigation {
    /** Busca também funciona sem acentos (ex.: "sao" encontra "São"). */
    fun matches(text: String, query: String): Boolean {
        val searched = normalize(query.trim())
        return searched.isEmpty() || normalize(text).contains(searched)
    }

    private fun normalize(text: String): String = Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)

    /** Sem salto circular: no primeiro/último item, o botão de navegação desabilita. */
    fun adjacentIndex(current: Int, count: Int, step: Int): Int? {
        if (current !in 0 until count || step !in listOf(-1, 1)) return null
        val target = current + step
        return target.takeIf { it in 0 until count }
    }

    /** Nunca cria legenda com duração negativa, fora do vídeo ou de 0 ms. */
    fun newInterval(durationMs: Long, afterMs: Long?): Pair<Long, Long>? {
        if (durationMs <= 0L) return null
        val start = (afterMs ?: 0L).coerceIn(0L, durationMs - 1L)
        return start to minOf(durationMs, start + minOf(3000L, durationMs - start))
    }

    /** Rejeita arquivos muito grandes antes de retornar qualquer texto ao importador. */
    fun readSrtBounded(reader: Reader, maxCharacters: Int = 250_000): String {
        require(maxCharacters >= 0)
        val output = StringBuilder()
        val chunk = CharArray(4096)
        while (output.length <= maxCharacters) {
            val read = reader.read(chunk, 0, minOf(chunk.size, maxCharacters + 1 - output.length))
            if (read <= 0) break
            output.append(chunk, 0, read)
        }
        require(output.length <= maxCharacters) {
            "SRT maior que $maxCharacters caracteres. Divida o arquivo antes de importar."
        }
        return output.toString()
    }

}
