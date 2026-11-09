package br.com.uaicortes

import android.accounts.Account
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

/** Apenas metadado de associação local, nunca token. O Google pode exigir consentimento novamente. */

internal enum class YouTubePermission { READ, PUBLISH }

/** O callback recebe um token que deve existir somente em RAM. */
internal class YouTubeAuthorizationAction(
    private val start: (Boolean, YouTubePermission, String?, (String) -> Unit) -> Unit
) {
    /** Escolha explícita para cadastro de outro canal; leitura não pede youtube.upload. */
    operator fun invoke(chooseAccount: Boolean, onToken: (String) -> Unit) =
        start(chooseAccount, YouTubePermission.READ, null, onToken)

    /** Canal já associado: tenta silenciosamente a conta indicada. */
    fun forChannel(accountEmail: String, onToken: (String) -> Unit) =
        start(false, YouTubePermission.READ, accountEmail, onToken)

    /** Escrita somente quando o usuário confirma um envio; ainda consulta channel(mine=true). */
    fun forUpload(accountEmail: String?, onToken: (String) -> Unit) =
        start(accountEmail.isNullOrBlank(), YouTubePermission.PUBLISH, accountEmail, onToken)
}

/** Autorização Google no Android, sem webview, client_secret ou credenciais persistidas. */
@Composable
internal fun rememberYouTubeAuthorization(onError: (String) -> Unit): YouTubeAuthorizationAction {
    val context = LocalContext.current
    val activity = context as? Activity
    val callback = remember { mutableStateOf<((String) -> Unit)?>(null) }
    val onErrorNow by rememberUpdatedState(onError)
    val client = remember(activity) { activity?.let { Identity.getAuthorizationClient(it) } }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val action = callback.value
        callback.value = null
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            onErrorNow("Autorização do YouTube não concluída.")
        } else {
            runCatching { client?.getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { grant ->
                    val token = grant?.accessToken
                    if (!token.isNullOrBlank() && action != null) action(token)
                    else onErrorNow("Nenhum token YouTube autorizado foi retornado.")
                }.onFailure { onErrorNow("Falha ao concluir autorização do YouTube.") }
        }
    }
    return YouTubeAuthorizationAction { chooseAccount, permission, rawAccountEmail, next ->
        if (client == null) {
            onErrorNow("Não foi possível acessar a autorização Google neste aparelho.")
        } else if (callback.value != null) {
            onErrorNow("Já existe uma autorização em andamento.")
        } else {
            val accountEmail = rawAccountEmail?.let(YouTubeGoogleAccountEmail::normalized)
            if (accountEmail != null && !YouTubeGoogleAccountEmail.valid(accountEmail)) {
                onErrorNow("O e-mail da conta Google vinculada é inválido. Revise em Minhas contas.")
            } else {
                callback.value = next
                val scopes = listOf(Scope("https://www.googleapis.com/auth/youtube.readonly")) +
                    (if (permission == YouTubePermission.PUBLISH)
                        listOf(Scope("https://www.googleapis.com/auth/youtube.upload"))
                    else emptyList())
                val builder = AuthorizationRequest.builder().setRequestedScopes(scopes)
                // setAccount escolhe a conta correta, quando previamente autorizada.
                // SELECT_ACCOUNT substitui setAccount: os dois nunca devem ser usados juntos.
                if (!accountEmail.isNullOrBlank()) {
                    builder.setAccount(Account(accountEmail, "com.google"))
                } else if (chooseAccount) {
                    builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
                }
                client.authorize(builder.build()).addOnSuccessListener { grant ->
                    if (grant.hasResolution()) {
                        val intent = grant.pendingIntent
                        if (intent == null) {
                            callback.value = null
                            onErrorNow("Autorização Google indisponível.")
                        } else runCatching {
                            launcher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
                        }.onFailure {
                            callback.value = null
                            onErrorNow("Não foi possível abrir a autorização Google.")
                        }
                    } else {
                        val task = callback.value
                        callback.value = null
                        val token = grant.accessToken
                        if (token.isNullOrBlank() || task == null)
                            onErrorNow("A conta não concedeu os escopos solicitados.")
                        else task(token)
                    }
                }.addOnFailureListener {
                    callback.value = null
                    onErrorNow("Falha de autorização Google. Verifique a conta vinculada e os Serviços Google Play.")
                }
            }
        }
    }
}
