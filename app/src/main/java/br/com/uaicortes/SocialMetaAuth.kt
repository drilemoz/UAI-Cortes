package br.com.uaicortes

import android.content.Intent
import androidx.activity.ComponentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

/**
 * Sessão Meta via SDK oficial. Identificadores públicos podem ser salvos no SocialStore;
 * tokens de usuário/Página NÃO entram no catálogo, backups, logs ou textos da UI.
 * Tokens de Página permanecem somente na memória deste processo.
 * O SDK da Meta gerencia sua própria sessão de usuário; Desconectar a encerra.
 */
internal object SocialMetaAuth {
    private val callbackManager: CallbackManager by lazy { CallbackManager.Factory.create() }
    private var pending: ((Boolean, String) -> Unit)? = null
    private val pageTokens = mutableMapOf<String, String>()

    val isConfigured: Boolean
        get() = BuildConfig.META_APP_ID.matches(Regex("[0-9]{5,30}")) &&
            BuildConfig.META_CLIENT_TOKEN.isNotBlank()

    fun currentUserToken(): String? = if (isConfigured) {
        AccessToken.getCurrentAccessToken()?.takeUnless { it.isExpired }?.token
    } else null

    fun canPublish(): Boolean = if (isConfigured) {
        AccessToken.getCurrentAccessToken()?.takeUnless { it.isExpired }
            ?.permissions?.contains("pages_manage_posts") == true
    } else false

    @Synchronized
    fun start(activity: ComponentActivity, publish: Boolean, onDone: (Boolean, String) -> Unit) {
        if (!isConfigured) {
            onDone(false, "Configure META_APP_ID e META_CLIENT_TOKEN no build; registre o hash da assinatura na Meta.")
            return
        }
        if (pending != null) {
            onDone(false, "Já existe uma autorização Meta em andamento.")
            return
        }
        pageTokens.clear() // troca de conta invalida a sessão de Páginas anterior
        pending = onDone
        try {
            val manager = LoginManager.getInstance()
            manager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    complete(true, "Meta autorizada. Consulte as Páginas disponíveis.")
                }
                override fun onCancel() { complete(false, "Autorização Meta cancelada.") }
                override fun onError(error: FacebookException) {
                    // A mensagem do SDK pode conter detalhes privados, não exibir/logar.
                    complete(false, "Não foi possível autorizar a Meta. Confira app, permissões e assinatura.")
                }
            })
            if (publish) {
                manager.logInWithPublishPermissions(activity, listOf("pages_manage_posts"))
            } else {
                manager.logInWithReadPermissions(activity, listOf("pages_show_list", "pages_read_engagement"))
            }
        } catch (error: Exception) {
            complete(false, "Falha ao abrir autorização Meta. Verifique as configurações do aplicativo.")
        }
    }

    @Synchronized
    private fun complete(ok: Boolean, message: String) {
        val callback = pending
        pending = null
        callback?.invoke(ok, message)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean =
        if (isConfigured) callbackManager.onActivityResult(requestCode, resultCode, data) else false

    @Synchronized
    fun putPageTokens(pages: List<SocialExternalApis.FacebookPage>) {
        pageTokens.clear()
        if (currentUserToken() != null) pages.forEach { page ->
            if (page.accessToken.isNotBlank()) pageTokens[page.id] = page.accessToken
        }
    }

    @Synchronized
    fun pageToken(pageId: String): String? =
        if (canPublish()) pageTokens[pageId] else null

    @Synchronized
    fun disconnect() {
        pageTokens.clear()
        pending = null
        if (isConfigured) LoginManager.getInstance().logOut()
    }
}
