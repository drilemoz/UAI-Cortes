package br.com.uaicortes

import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal data class GeminiVoiceCatalogPage(val voices: List<GeminiVoice>, val nextPageToken: String)
internal data class GeminiVoiceCatalogResult(val voices: List<GeminiVoice>, val report: List<String>)

/** Erros sempre resumidos: jamais incluir URL, chave, resposta bruta ou transcrição em diagnósticos. */
internal class GeminiVoiceCatalogFailure(
    val stage: String, val causeError: Exception, val previous: List<String>
) : IOException("Falha em $stage: ${GeminiVoiceCatalog.safeMessage(causeError)}", causeError) {
    fun report(): List<String> = previous + listOf(message.orEmpty())
}

internal object GeminiVoiceCatalog {
    fun shouldFallbackOnHttp(code: Int): Boolean = code == 400 || code == 422

    /** Permite testes offline com respostas simuladas, exercitando o mesmo caminho de produção. */
    suspend fun fetchCatalog(fetch: suspend (String) -> JSONObject): GeminiVoiceCatalogResult {
        val report = mutableListOf<String>()
        var filterRejected = false
        val localized = try {
            val list = fetchPages(true, fetch)
            report += "Filtro pt-BR: ${list.size} voz(es)."
            list
        } catch (e: GeminiApiError) {
            if (!shouldFallbackOnHttp(e.http)) throw GeminiVoiceCatalogFailure("Filtro pt-BR", e, report)
            filterRejected = true
            report += "Filtro pt-BR recusado (HTTP ${e.http}); consultando catálogo geral."
            emptyList()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { throw GeminiVoiceCatalogFailure("Filtro pt-BR", e, report) }
        if (localized.isNotEmpty()) return GeminiVoiceCatalogResult(localized, report)
        val general = try { fetchPages(false, fetch) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { throw GeminiVoiceCatalogFailure("Catálogo geral", e, report) }
        report += "Catálogo geral: ${general.size} voz(es)."
        if (general.isEmpty()) report += "API respondeu sem vozes utilizáveis."
        else if (filterRejected) report += "Resultado geral após rejeição do filtro."
        else report += "Sem vozes pt-BR; catálogo geral disponível (idioma não presumido)."
        return GeminiVoiceCatalogResult(general, report)
    }

    private suspend fun fetchPages(ptBrOnly: Boolean, fetch: suspend (String) -> JSONObject): List<GeminiVoice> {
        val voices = mutableListOf<GeminiVoice>()
        val visited = mutableSetOf<String>()
        var token = ""
        repeat(5) {
            val response = fetch(path(ptBrOnly, token))
            val page = parsePage(response)
            voices += page.voices
            token = page.nextPageToken
            if (token.isBlank()) return voices.distinctBy { it.id }
            if (!visited.add(token)) throw IOException("Paginação repetida no catálogo Gemini.")
        }
        return voices.distinctBy { it.id }
    }

    fun path(ptBrOnly: Boolean, token: String = ""): String = buildString {
        append("voices?page_size=100")
        if (ptBrOnly) append("&language_code=pt-BR")
        if (token.isNotBlank()) {
            require(token.length <= 2048 && token.none { it.isISOControl() })
            append("&page_token=")
            append(java.net.URLEncoder.encode(token, "UTF-8"))
        }
    }

    fun parsePage(response: JSONObject): GeminiVoiceCatalogPage {
        val list = response.optJSONArray("voices")
            ?: return GeminiVoiceCatalogPage(emptyList(), response.optString("next_page_token"))
        val result = ArrayList<GeminiVoice>(list.length())
        for (i in 0 until list.length()) {
            val row = list.optJSONObject(i) ?: continue
            val id = row.optString("id").trim()
            if (id.isBlank() || id.length > 150 || id.any { it.isISOControl() }) continue
            result += GeminiVoice(id,
                row.optString("display_name", id).ifBlank { id }.take(120),
                row.optString("description").take(300), row.optString("language_code").take(30),
                row.optString("accent").take(80), row.optString("persona").take(100),
                row.optString("context").take(80), row.optString("gender").take(30),
                row.optString("pitch").take(30))
        }
        return GeminiVoiceCatalogPage(result.distinctBy { it.id }, response.optString("next_page_token"))
    }

    fun safeMessage(error: Exception): String = when (error) {
        is GeminiApiError -> "HTTP ${error.http}: ${error.message.orEmpty()}"
        is UnknownHostException -> "Sem acesso ao servidor (DNS/rede)."
        is SocketTimeoutException -> "Tempo esgotado ao consultar o serviço."
        is java.net.ConnectException -> "Não foi possível conectar ao servidor."
        is javax.net.ssl.SSLException -> "Falha na conexão segura (TLS)."
        is org.json.JSONException -> "Resposta JSON inválida da API."
        is IOException -> when (error.message) {
            "Sem conexão com a internet." -> error.message.orEmpty()
            "A conexão com Gemini excedeu o tempo limite." -> error.message.orEmpty()
            "Resposta maior que o limite permitido." -> error.message.orEmpty()
            "Paginação repetida no catálogo Gemini." -> error.message.orEmpty()
            else -> "Erro de rede ou resposta inesperada."
        }
        else -> "Falha local ao consultar; verifique a chave nas Configurações."
    }
}
