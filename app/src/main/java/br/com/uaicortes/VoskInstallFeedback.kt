package br.com.uaicortes

import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Código controlado para diagnóstico local; nunca inclui URI, nome de arquivo ou resposta HTTP. */
internal object VoskInstallFeedback {
    fun code(error: Throwable): String = when (error) {
        is VoskArchiveInstaller.InstallException -> error.problem.name.lowercase()
        is SocketTimeoutException -> "network_timeout"
        is UnknownHostException -> "dns_unavailable"
        is SecurityException -> "file_permission"
        is FileNotFoundException -> "file_not_found"
        is IOException -> "read_or_storage_failure"
        else -> "unexpected_failure"
    }

    fun message(error: Throwable): String = when (error) {
        is VoskArchiveInstaller.InstallException -> when (error.problem) {
            VoskArchiveInstaller.Problem.INVALID_ZIP -> "Arquivo ZIP inválido ou danificado. Baixe o modelo completo novamente."
            VoskArchiveInstaller.Problem.WRONG_MODEL -> "O ZIP selecionado não contém o modelo português esperado."
            VoskArchiveInstaller.Problem.UNSAFE_PATH,
            VoskArchiveInstaller.Problem.DUPLICATE_ENTRY -> "ZIP rejeitado por conter entradas inseguras ou repetidas."
            VoskArchiveInstaller.Problem.TOO_MANY_ENTRIES,
            VoskArchiveInstaller.Problem.TOO_LARGE -> "ZIP rejeitado por ultrapassar o limite de segurança."
            VoskArchiveInstaller.Problem.CHECKSUM -> "O ZIP está incompleto ou corrompido. Baixe novamente."
            VoskArchiveInstaller.Problem.IO -> "Não foi possível extrair o ZIP. Verifique o espaço disponível."
            VoskArchiveInstaller.Problem.INSTALL_FAILED -> "Falha ao finalizar a instalação. O modelo anterior foi preservado quando possível."
        }
        is SocketTimeoutException -> "A conexão demorou a responder. Tente novamente ou importe um ZIP completo."
        is UnknownHostException -> "O servidor do modelo não foi localizado. Verifique a conexão."
        is SecurityException -> "Sem permissão para ler o arquivo. Selecione o ZIP novamente."
        is FileNotFoundException -> "O arquivo selecionado não está disponível. Escolha-o novamente."
        else -> "A instalação falhou. Confira o arquivo, a conexão e o espaço livre."
    }
}
