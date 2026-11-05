package br.com.uaicortes

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

/** Autorização Google no próprio Android, sem webview, client_secret ou token persistido. */
@Composable
internal fun rememberYouTubeAuthorization(onError: (String) -> Unit): ((String) -> Unit) -> Unit {
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
    return { next ->
        if (client == null) {
            onErrorNow("Não foi possível acessar a autorização Google neste aparelho.")
        } else if (callback.value != null) {
            onErrorNow("Já existe uma autorização em andamento.")
        } else {
            callback.value = next
            val request = AuthorizationRequest.builder().setRequestedScopes(listOf(
                Scope("https://www.googleapis.com/auth/youtube.upload"),
                Scope("https://www.googleapis.com/auth/youtube.readonly")
            )).build()
            client.authorize(request).addOnSuccessListener { grant ->
                if (grant.hasResolution()) {
                    val intent = grant.pendingIntent
                    if (intent == null) {
                        callback.value = null; onErrorNow("Autorização Google indisponível.")
                    } else runCatching {
                        launcher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
                    }.onFailure {
                        callback.value = null; onErrorNow("Não foi possível abrir a autorização Google.")
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
                onErrorNow("Falha de autorização Google. Confira a configuração OAuth e os Serviços Google Play.")
            }
        }
    }
}
