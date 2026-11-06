package br.com.uaicortes

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * Instalador JVM do modelo de fala. Não acessa rede ou áudio.
 * Recebe um ZIP já copiado para o armazenamento privado e só publica arquivos
 * depois de validar integralmente a extração em diretório temporário.
 */
internal object VoskArchiveInstaller {
    enum class Problem {
        INVALID_ZIP, WRONG_MODEL, UNSAFE_PATH, DUPLICATE_ENTRY, TOO_MANY_ENTRIES,
        TOO_LARGE, CHECKSUM, IO, INSTALL_FAILED
    }

    class InstallException(val problem: Problem, message: String, cause: Throwable? = null) :
        IOException(message, cause)

    data class Result(val files: Int, val bytes: Long, val format: VoskModelLayout.Format)

    private const val MAX_ENTRIES = 4000
    private val rootEntries = setOf(
        "final.mdl", "mfcc.conf", "HCLr.fst", "Gr.fst", "HCLG.fst",
        "am", "conf", "graph", "ivector", "phones.txt", "word_boundary.int",
        "disambig_tid.int", "README", "COPYING", "rescore", "rnnlm"
    )

    /**
     * Suporta ZIP oficial modelo/, ZIP extraído sem pasta externa, ou wrapper/modelo/.
     * Não aceita outro nome de modelo, caminhos fora da raiz ou entradas duplicadas.
     */
    fun extract(
        zipFile: File,
        destinationRoot: File,
        modelName: String,
        maxExtractedBytes: Long,
        checkpoint: () -> Unit = {}
    ): Result {
        if (!zipFile.isFile || zipFile.length() == 0L) {
            throw InstallException(Problem.INVALID_ZIP, "Arquivo ZIP vazio ou indisponível.")
        }
        if (destinationRoot.exists() && !destinationRoot.deleteRecursively()) {
            throw InstallException(Problem.IO, "Não foi possível limpar a extração anterior.")
        }
        if (!destinationRoot.mkdirs()) {
            throw InstallException(Problem.IO, "Não foi possível preparar o armazenamento do modelo.")
        }
        val root = destinationRoot.canonicalFile
        var bytes = 0L
        var files = 0
        var entries = 0
        val written = mutableSetOf<String>()

        try {
            ZipFile(zipFile).use { archive ->
                val items = archive.entries()
                while (items.hasMoreElements()) {
                    checkpoint()
                    val item = items.nextElement()
                    entries++
                    if (entries > MAX_ENTRIES) {
                        throw InstallException(Problem.TOO_MANY_ENTRIES, "O ZIP contém arquivos demais.")
                    }
                    val original = item.name
                    if (original.length > 1024 || original.startsWith('/') || original.startsWith('\\') ||
                        original.contains('\u0000') || Regex("^[A-Za-z]:").containsMatchIn(original)) {
                        throw InstallException(Problem.UNSAFE_PATH, "O ZIP contém caminhos inseguros.")
                    }
                    val parts = original.replace('\\', '/').split('/').filter { it.isNotEmpty() }
                    if (parts.any { it == "." || it == ".." }) {
                        throw InstallException(Problem.UNSAFE_PATH, "O ZIP contém caminhos inseguros.")
                    }
                    val index = parts.indexOf(modelName)
                    val relative = when {
                        index in 0..1 -> parts.drop(index + 1)
                        index == -1 && parts.firstOrNull() in rootEntries -> parts
                        else -> emptyList()
                    }
                    if (relative.isEmpty()) continue // diretório do modelo ou metadados externos
                    val path = relative.joinToString("/")
                    val output = File(root, path).canonicalFile
                    if (!output.path.startsWith(root.path + File.separator)) {
                        throw InstallException(Problem.UNSAFE_PATH, "O ZIP contém caminhos inseguros.")
                    }
                    if (item.isDirectory) {
                        if (!output.isDirectory && !output.mkdirs()) {
                            throw InstallException(Problem.IO, "Não foi possível criar as pastas do modelo.")
                        }
                        continue
                    }
                    if (!written.add(path)) {
                        throw InstallException(Problem.DUPLICATE_ENTRY, "O ZIP contém arquivos repetidos.")
                    }
                    if (item.size > maxExtractedBytes - bytes) {
                        throw InstallException(Problem.TOO_LARGE, "O modelo excede o espaço máximo permitido.")
                    }
                    if (output.exists() || output.parentFile?.mkdirs() == false &&
                        output.parentFile?.isDirectory != true) {
                        throw InstallException(Problem.IO, "Não foi possível criar o arquivo do modelo.")
                    }
                    val crc = CRC32()
                    archive.getInputStream(item).use { input ->
                        FileOutputStream(output).use { out ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                checkpoint()
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (count == 0) continue
                                if (count > maxExtractedBytes - bytes) {
                                    throw InstallException(Problem.TOO_LARGE, "O modelo descompactado é grande demais.")
                                }
                                out.write(buffer, 0, count)
                                crc.update(buffer, 0, count)
                                bytes += count
                            }
                        }
                    }
                    if ((item.size >= 0L && output.length() != item.size) ||
                        (item.crc >= 0L && crc.value != item.crc)) {
                        throw InstallException(Problem.CHECKSUM, "O ZIP está incompleto ou corrompido.")
                    }
                    files++
                }
            }
        } catch (e: InstallException) {
            throw e
        } catch (e: ZipException) {
            throw InstallException(Problem.INVALID_ZIP, "O arquivo não é um ZIP válido ou está corrompido.", e)
        } catch (e: IOException) {
            throw InstallException(Problem.IO, "Não foi possível ler ou extrair o ZIP.", e)
        }
        val format = VoskModelLayout.detect(root)
            ?: throw InstallException(Problem.WRONG_MODEL,
                "Não foi encontrado um modelo Vosk português compatível no ZIP.")
        return Result(files, bytes, format)
    }

    /**
     * A troca ocorre apenas depois de validar a pasta temporária.
     * Um modelo já funcional nunca é substituído. Mantém backup do destino
     * incompleto até a nova pasta entrar com sucesso e restaura em caso de falha.
     */
    fun installValidated(unpack: File, target: File): Boolean {
        if (VoskModelLayout.detect(unpack) == null) {
            throw InstallException(Problem.WRONG_MODEL, "O modelo extraído está incompleto.")
        }
        if (VoskModelLayout.detect(target) != null) return false
        val backup = File(target.parentFile, "${target.name}.install-previous")
        if (backup.exists()) {
            if (VoskModelLayout.detect(backup) != null) {
                // Interrupção de uma instalação anterior: restaurar o modelo funcional.
                if (target.exists() && !target.deleteRecursively()) {
                    throw InstallException(Problem.INSTALL_FAILED, "Não foi possível restaurar o modelo anterior.")
                }
                if (!backup.renameTo(target)) {
                    throw InstallException(Problem.INSTALL_FAILED, "Não foi possível restaurar o modelo anterior.")
                }
                return false
            }
            if (!backup.deleteRecursively()) {
                throw InstallException(Problem.INSTALL_FAILED, "Não foi possível limpar a instalação antiga.")
            }
        }
        val hadTarget = target.exists()
        if (hadTarget && !target.renameTo(backup)) {
            throw InstallException(Problem.INSTALL_FAILED, "Não foi possível reservar a instalação antiga.")
        }
        if (!unpack.renameTo(target)) {
            if (hadTarget && !backup.renameTo(target)) {
                throw InstallException(Problem.INSTALL_FAILED,
                    "Instalação incompleta. Não foi possível restaurar o modelo anterior.")
            }
            throw InstallException(Problem.INSTALL_FAILED, "Não foi possível concluir a instalação.")
        }
        if (VoskModelLayout.detect(target) == null) {
            target.deleteRecursively()
            if (hadTarget) backup.renameTo(target)
            throw InstallException(Problem.INSTALL_FAILED, "A verificação final do modelo falhou.")
        }
        if (hadTarget) backup.deleteRecursively()
        return true
    }
}
