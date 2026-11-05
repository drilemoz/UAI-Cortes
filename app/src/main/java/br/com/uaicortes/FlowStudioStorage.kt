package br.com.uaicortes

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Arquivos privados por projeto: não altera o JSON dos projetos de vídeo ou o catálogo Flow legado. */
internal class FlowStudioStorage(private val context: Context, projectId: String) {
    private val key = MessageDigest.getInstance("SHA-256").digest(projectId.toByteArray())
        .take(16).joinToString("") { "%02x".format(it) }
    private val root = File(context.filesDir, "flow_studio_v1").apply { mkdirs() }
    private val data = AtomicFile(File(root, "$key.json"))
    private val assets = File(root, "$key-assets").apply { mkdirs() }

    @Synchronized fun read(): FlowStudioDraft? {
        if (!data.baseFile.exists()) return null
        val bytes = data.openRead().use { it.readBytesLimited(1024 * 1024) }
        return FlowStudioDraft.read(JSONObject(String(bytes, Charsets.UTF_8)))
    }

    @Synchronized fun save(draft: FlowStudioDraft) {
        // Nunca substituir silenciosamente um arquivo anterior ilegível.
        read()
        require(draft.scenes.size <= 60 && draft.people.size <= 30 && draft.places.size <= 30)
        val bytes = draft.json().toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 1024 * 1024) { "Rascunho acima do limite de 1 MiB." }
        var stream: FileOutputStream? = null
        try {
            stream = data.startWrite()
            stream.write(bytes)
            data.finishWrite(stream)
        } catch (failure: Exception) {
            stream?.let { data.failWrite(it) }
            throw failure
        }
    }

    /** Só após exclusão explícita do catálogo; não remove vídeos ou arquivos da galeria. */
    @Synchronized fun deleteAll(): Boolean {
        data.delete()
        return assets.deleteRecursively() && !data.baseFile.exists()
    }

    fun asset(name: String): File? {
        if (!name.matches(Regex("[a-fA-F0-9-]{36}\\.(png|jpg|webp)"))) return null
        return File(assets, name).takeIf { it.isFile && it.length() in 1..12_000_000 }
    }

    /** Cópia de mídia escolhida via SAF: evita permissões persistentes após reiniciar. */
    fun importImage(sceneId: String, uri: Uri): String {
        require(sceneId.matches(Regex("[a-fA-F0-9-]{36}")))
        return context.contentResolver.openInputStream(uri)?.use { stream -> saveImage(sceneId, stream.readBytesLimited(12_000_000)) }
            ?: throw IOException("Não foi possível ler a imagem escolhida.")
    }

    fun saveImage(sceneId: String, bytes: ByteArray): String {
        require(sceneId.matches(Regex("[a-fA-F0-9-]{36}")))
        require(bytes.size in 100..12_000_000) { "Imagem vazia ou maior que 12 MB." }
        val ext = when {
            bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() -> "jpg"
            bytes.size >= 8 && bytes.take(4).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47)) -> "png"
            bytes.size >= 12 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WEBP" -> "webp"
            else -> throw IOException("Formato de imagem não reconhecido (JPEG, PNG ou WebP).")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..10000 && bounds.outHeight in 1..15000) { "Imagem não decodificável ou dimensões inseguras." }
        val target = File(assets, "$sceneId.$ext")
        val temporary = File.createTempFile("studio-", ".part", assets)
        try {
            temporary.outputStream().use { it.write(bytes); it.flush() }
            if (!temporary.renameTo(target)) throw IOException("Falha ao salvar imagem interna.")
            listOf("png", "jpg", "webp").filterNot { it == ext }.forEach { File(assets, "$sceneId.$it").delete() }
        } finally { temporary.delete() }
        return target.name
    }

    fun copyImage(name: String, destination: Uri) {
        val from = asset(name) ?: throw IOException("Imagem não disponível neste aparelho.")
        context.contentResolver.openOutputStream(destination, "wt")?.use { output -> from.inputStream().use { it.copyTo(output) } }
            ?: throw IOException("Não foi possível abrir o destino de exportação.")
    }

    fun exportZip(destination: Uri, draft: FlowStudioDraft) {
        context.contentResolver.openOutputStream(destination, "wt")?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                fun entry(name: String, text: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                entry("roteiro-completo.txt", draft.story.fullText() + "\nRoteiro original:\n" + draft.story.originalScript)
                entry("personagens.json", org.json.JSONArray().also { arr -> draft.people.forEach { arr.put(it.json()) } }.toString(2))
                entry("cenarios.json", org.json.JSONArray().also { arr -> draft.places.forEach { arr.put(it.json()) } }.toString(2))
                entry("ordem-e-continuidade.json", draft.json(includeLocalUri = false).toString(2))
                draft.scenes.forEachIndexed { index, scene ->
                    val prefix = "cenas/cena-${(index + 1).toString().padStart(2, '0')}/"
                    entry("${prefix}prompt.txt", scene.prompt)
                    entry("${prefix}roteiro.txt", listOf("Título: ${scene.title}", "Visual: ${scene.visual}",
                        "Personagens: ${scene.characters}", "Ações: ${scene.actions}",
                        "Falas (pt-BR): ${scene.effectiveDialogue()}", "Duração estimada: ${scene.durationSeconds}s", "Sons: ${scene.effects}",
                        "Câmera: ${scene.camera}", "Estado inicial: ${scene.initialState}",
                        "Estado final: ${scene.finalState}", "Vídeo: ${scene.videoName}").joinToString("\n"))
                    if (scene.imageAsset.isNotBlank()) {
                        val file = asset(scene.imageAsset) ?: throw IOException("Imagem ausente na cena ${index + 1}.")
                        zip.putNextEntry(ZipEntry("${prefix}imagem.${file.extension}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        } ?: throw IOException("Destino de ZIP indisponível.")
    }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val buffer = ByteArray(8192)
    val out = java.io.ByteArrayOutputStream()
    while (true) {
        val n = read(buffer)
        if (n == -1) break
        if (out.size() + n > limit) throw IOException("Arquivo ultrapassa o limite de segurança.")
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
