package br.com.uaicortes

import android.content.Context
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Erro HTTP tipado, sem body/prompt/chave. Usado para identificar 429 sem procurar strings localizadas. */
internal class GeminiHttpException(val statusCode: Int, text: String) : IOException(text)

/**
 * Retentativa limitada a uma operação autorizada pelo usuário. O app não cria projetos/chaves,
 * não tenta contornar limites do mesmo projeto e não faz chamadas em segundo plano.
 */
internal class GeminiKeyFailover(context: Context) {
    private val store = GeminiCredentialStore(context)
    suspend fun <T> run(initialKey: String, model: String = "", action: suspend (String) -> T): T {
        val profiles = store.profiles()
        val configured = profiles.firstOrNull { runCatching { store.readProfile(it.id) == initialKey }.getOrDefault(false) }
        // Após um failover anterior, as próximas chamadas da mesma ação usam a chave ativa.
        val source = if (configured != null && store.autoFallbackEnabled())
            profiles.firstOrNull { it.id == store.activeId() } ?: configured else configured
        val selectedKey = source?.let { store.readProfile(it.id) } ?: initialKey
        if (source == null || source.projectId.isBlank() || !store.autoFallbackEnabled()) return action(selectedKey)
        val triedProjects = mutableSetOf<String>()
        var lastError: Exception? = null
        for (candidate in listOf(source) + profiles.filterNot { it.id == source.id }) {
            val sameProject = candidate.projectId == source.projectId
            if (candidate.projectId.isBlank() || candidate.projectId in triedProjects ||
                store.temporarilyBlocked(candidate.projectId, model)) continue
            if (!sameProject && !GeminiFailoverPolicy.canSwitch(429, true, source.projectId,
                    candidate.projectId, triedProjects, false)) continue
            triedProjects += candidate.projectId
            val credential = if (candidate.id == source.id) selectedKey else store.readProfile(candidate.id)
            if (credential == null) continue
            try {
                val result = action(credential)
                if (candidate.id != source.id) {
                    store.select(candidate.id)
                    store.markAutomaticSwitch(candidate.id)
                }
                return result
            } catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) {
                if (!quota429(error)) throw error
                store.markExhausted(candidate.projectId, model)
                lastError = error
            }
        }
        // Sem nova chamada de rede se todos os projetos já estão pausados.
        throw lastError ?: GeminiHttpException(429,
            "Os projetos Gemini cadastrados estão temporariamente em pausa após limite de uso. Aguarde ou verifique suas cotas.")
    }
    private fun quota429(error: Exception): Boolean = when (error) {
        is GeminiApiError -> error.http == 429
        is GeminiHttpException -> error.statusCode == 429
        else -> false
    }
}
