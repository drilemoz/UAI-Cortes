package br.com.uaicortes

/** Validação local de e-mail, sem ler contas ou credenciais do Android. */
internal object YouTubeGoogleAccountEmail {
    private val emailPattern = Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$")
    fun normalized(raw: String): String = raw.trim().lowercase(java.util.Locale.ROOT)
    fun valid(raw: String): Boolean = raw.length in 5..254 &&
        !raw.any(Char::isWhitespace) && emailPattern.matches(raw)
}

