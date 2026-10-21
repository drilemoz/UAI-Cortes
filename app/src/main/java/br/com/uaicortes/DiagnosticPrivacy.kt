package br.com.uaicortes

/** Códigos seguros para relatórios: nunca persistir texto livre de usuário/arquivos/URIs. */
object DiagnosticPrivacy {
    private val codeRegex = Regex("ERROR_CODE_[A-Z0-9_]+")
    private val allowedScope = Regex("[^a-zA-Z0-9_.-]")

    fun safeIdentifier(value: String): String = value.replace(allowedScope, "_").take(56)

    fun errorCategory(raw: String): String = when {
        raw.contains("width and height must be > 0", ignoreCase = true) -> "INVALID_FRAME_DIMENSIONS"
        raw.contains("asset loader", ignoreCase = true) -> "ASSET_LOADER"
        raw.contains("permission", ignoreCase = true) || raw.contains("securityexception", ignoreCase = true) -> "MEDIA_PERMISSION"
        raw.contains("no space", ignoreCase = true) || raw.contains("enospc", ignoreCase = true) -> "NO_STORAGE"
        raw.contains("codec", ignoreCase = true) || raw.contains("decoder", ignoreCase = true) -> "CODEC"
        raw.contains("timeout", ignoreCase = true) -> "TIMEOUT"
        else -> "UNCLASSIFIED"
    }

    fun safeFailure(raw: String): String {
        val code = codeRegex.find(raw)?.value ?: "NO_MEDIA3_CODE"
        return "$code reason=${errorCategory(raw)}"
    }

    /** Posições aproximadas em passos de 5%, sem coordenadas absolutas nem texto tocado. */
    fun touchGrid(x: Float, y: Float, width: Int, height: Int): String {
        fun bucket(v: Float, max: Int): Int =
            if (max <= 0 || !v.isFinite()) 0 else ((v / max * 20f).toInt().coerceIn(0, 20) * 5)
        return "x=${bucket(x, width)}% y=${bucket(y, height)}%"
    }
}
