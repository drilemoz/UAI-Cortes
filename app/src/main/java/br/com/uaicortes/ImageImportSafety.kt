package br.com.uaicortes

import java.io.FileNotFoundException

/** Regras puras: confiar no conteúdo identificado pelo decodificador, não só no provedor. */
internal object ImageImportSafety {
    private val accepted = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/bmp")

    fun normalizeMime(raw: String?): String? {
        val normalized = raw?.substringBefore(';')?.trim()?.lowercase() ?: return null
        return when (normalized) {
            "image/jpg", "image/pjpeg" -> "image/jpeg"
            "image/x-ms-bmp", "image/x-bmp" -> "image/bmp"
            else -> normalized
        }
    }

    /** Se os bytes declararem um tipo, ele prevalece. GIF disfarçado de PNG não é aceito. */
    fun recognizedMime(provider: String?, decoded: String?): String? {
        val actual = normalizeMime(decoded)
        val declared = normalizeMime(provider)
        return when {
            actual != null -> actual.takeIf { it in accepted }
            declared != null -> declared.takeIf { it in accepted }
            else -> null
        }
    }

    /** Classificação sem texto livre para localizar falhas dos provedores nos relatórios. */
    fun diagnosticKind(error: Throwable): String {
        val causes = generateSequence(error) { it.cause }.take(5).toList()
        return when {
            causes.any { it is SecurityException } -> "permission"
            causes.any { it is FileNotFoundException } -> "missing"
            causes.any { it is IllegalArgumentException } -> "invalid_input"
            causes.any { it is IllegalStateException } -> "provider_state"
            else -> "io_or_unknown"
        }
    }

    /** Mensagens estáticas evitam revelar caminhos, conteúdo ou textos de provedores externos. */
    fun userMessage(error: Throwable): String {
        val causes = generateSequence(error) { it.cause }.take(5).toList()
        if (causes.any { it is SecurityException })
            return "Sem permissão para ler a foto. Selecione novamente pelo gerenciador de arquivos."
        if (causes.any { it is FileNotFoundException })
            return "A imagem não está mais acessível. Localize novamente o arquivo original."
        val known = setOf(
            "Formato não suportado. Escolha JPG, PNG, WebP, HEIC ou BMP (GIF não é aceito).",
            "Imagem vazia ou acima de 24 MB. Escolha um arquivo menor.",
            "Imagem inválida ou acima de 24 megapixels. Reduza a resolução.",
            "HEIC exige Android 9 ou mais recente. Escolha JPG ou PNG.",
            "Imagem acima de 24 MB.",
            "Escolha uma imagem da galeria ou do gerenciador de arquivos.",
        )
        return causes.mapNotNull { it.message }.firstOrNull { it in known }
            ?: "Não foi possível abrir essa imagem. Escolha outro arquivo JPG ou PNG e tente novamente."
    }
}
