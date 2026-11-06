package br.com.uaicortes

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Verifica duração, formato e permissão durável antes de alterar o projeto. */
internal object MediaRelinkVerification {
    suspend fun prepare(context: Context, uri: Uri, target: MediaRelinkEntry): String =
        withContext(Dispatchers.IO) {
            require(uri.scheme == "content") { "Selecione a mídia pelo armazenamento Android." }
            val resolver = context.contentResolver
            val mime = resolver.getType(uri)?.lowercase().orEmpty()
            require(mime.startsWith(target.mimePrefix)) {
                "O tipo não corresponde ao original (${target.mimePrefix.removeSuffix("/")})."
            }
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                require(descriptor.statSize != 0L) { "O arquivo escolhido está vazio." }
            } ?: error("O arquivo não pode ser aberto.")
            if (target.mimePrefix == "image/") {
                // Reutiliza a validação de tamanho, dimensões e formato da importação normal.
                val source = ImageClipImport.inspect(context, uri)
                // Uma cópia privada evita depender de uma concessão temporária da galeria.
                ImageClipImport.copyIntoProject(context, source).uri
            } else {
                val length = verifiedMediaDuration(context, uri)
                require(length > 0L) { "Não foi possível verificar a duração do arquivo." }
                require(length >= target.minimumSourceEndMs) {
                    "O arquivo é curto demais para os cortes salvos (${target.minimumSourceEndMs} ms necessários)."
                }
                try {
                    resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    error("Não foi possível manter o acesso ao arquivo. Selecione-o por Arquivos do Android.")
                }
                // A recuperação precisa sobreviver à reinicialização e revogação de acesso temporário.
                require(resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) {
                    "A permissão persistente não foi concedida pelo provedor."
                }
                uri.toString()
            }
        }
}
