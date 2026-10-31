package br.com.uaicortes

import java.io.File

/**
 * A biblioteca Vosk suporta tanto modelos antigos (arquivos na raiz) quanto os
 * modelos novos (am/conf/graph). O pacote oficial pt-0.3 usa o formato antigo.
 * Esta inspeção não substitui a inicialização do modelo nativo no aparelho.
 */
internal object VoskModelLayout {
    enum class Format { LEGACY, MODERN }

    fun detect(root: File): Format? {
        fun valid(path: String) = File(root, path).let { it.isFile && it.length() > 0L }
        val legacyGraph = valid("HCLG.fst") || (valid("HCLr.fst") && valid("Gr.fst"))
        val modernGraph = valid("graph/HCLG.fst") ||
            (valid("graph/HCLr.fst") && valid("graph/Gr.fst"))
        return when {
            valid("am/final.mdl") && valid("conf/model.conf") &&
                valid("conf/mfcc.conf") && modernGraph -> Format.MODERN
            valid("final.mdl") && valid("mfcc.conf") && legacyGraph -> Format.LEGACY
            else -> null
        }
    }
}
