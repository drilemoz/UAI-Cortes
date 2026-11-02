package br.com.uaicortes

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.json.JSONException

/** Mensagens sem expor URL privada, cabeçalhos, tokens ou corpo da resposta. */
internal object GitHubUpdateDiagnostics {
    fun http(status: Int, rateLimitRemaining: String? = null): String = when (status) {
        401 -> "GitHub HTTP 401: autenticação necessária. Este verificador só consulta Releases públicas."
        403 -> if (rateLimitRemaining == "0")
            "GitHub HTTP 403: limite de consultas da API atingido. Tente novamente mais tarde."
        else "GitHub HTTP 403: acesso negado. Verifique a visibilidade do repositório e os limites da API."
        404 -> "GitHub HTTP 404: repositório inexistente, privado ou sem acesso público. Confira dono/nome no navegador."
        429 -> "GitHub HTTP 429: muitas consultas. Tente novamente mais tarde."
        in 500..599 -> "GitHub HTTP $status: falha temporária no servidor. Tente novamente mais tarde."
        else -> "GitHub HTTP $status: não foi possível listar as Releases."
    }

    fun exception(error: Throwable): String = when (error) {
        is UnknownHostException -> "DNS: não foi possível localizar api.github.com. Verifique a conexão, DNS privado ou VPN."
        is SocketTimeoutException -> "Tempo limite ao consultar api.github.com. Verifique a rede e tente novamente."
        is SSLException -> "Falha de conexão segura (TLS/SSL) com o GitHub. Confira data/hora do Android e a rede."
        is ConnectException, is NoRouteToHostException ->
            "Conexão com api.github.com recusada ou sem rota. Verifique a rede, VPN ou firewall."
        is SocketException -> "Conexão interrompida durante a consulta ao GitHub. Tente novamente."
        is SecurityException -> "O Android bloqueou o acesso à rede para o aplicativo. Confira as permissões e restrições."
        is JSONException -> "O GitHub retornou dados que o aplicativo não conseguiu interpretar."
        is IllegalStateException -> "A resposta do GitHub não pôde ser processada. Tente novamente."
        else -> "Falha inesperada na consulta ao GitHub (${error.javaClass.simpleName.take(48)})."
    }
}
