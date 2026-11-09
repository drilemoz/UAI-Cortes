package br.com.uaicortes

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

/** Referências consentidas são privadas e reutilizadas em projetos diferentes. */
internal data class VoiceCloneProfile(
    val id: String, val name: String, val engine: String, val createdAt: Long
)

internal object VoiceClonePolicy {
    const val COQUI = "coqui_xtts_v2"
    const val CHATTERBOX = "chatterbox_v3"
    const val MAX_REFERENCE = 10L * 1024 * 1024
    const val MAX_OUTPUT = 24L * 1024 * 1024
    fun engineAllowed(engine: String) = engine == COQUI || engine == CHATTERBOX
    fun safeId(id: String) = Regex("[0-9a-f-]{36}").matches(id)
    fun checkEndpoint(input: String): String {
        val parsed = java.net.URI(input.trim())
        require(parsed.scheme == "https" && !parsed.host.isNullOrBlank() &&
            parsed.userInfo == null && parsed.query == null && parsed.fragment == null &&
            (parsed.path.isNullOrBlank() || parsed.path == "/")) {
            "Informe somente a origem HTTPS válida do servidor (sem caminho, usuário ou parâmetros)."
        }
        return parsed.toString().trimEnd('/')
    }
}

internal class VoiceCloneLibrary(context: Context) {
    private val folder = File(context.applicationContext.filesDir, "voice_clone_refs_v1")
    private val manifest = AtomicFile(File(folder, "profiles.json"))
    fun reference(profile: VoiceCloneProfile): File {
        require(VoiceClonePolicy.safeId(profile.id))
        return File(folder, "${profile.id}.wav")
    }
    @Synchronized fun load(): List<VoiceCloneProfile> = runCatching {
        if (!manifest.baseFile.isFile || manifest.baseFile.length() !in 2..120_000L) return emptyList()
        val root = JSONObject(manifest.openRead().bufferedReader().use { it.readText() })
        val array = root.optJSONArray("voices") ?: JSONArray()
        (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            val profile = VoiceCloneProfile(item.optString("id"), item.optString("name"),
                item.optString("engine"), item.optLong("createdAt"))
            profile.takeIf { VoiceClonePolicy.safeId(it.id) && VoiceClonePolicy.engineAllowed(it.engine) &&
                it.name.isNotBlank() && reference(it).isFile }
        }.distinctBy { it.id }.take(100)
    }.getOrDefault(emptyList())

    @Synchronized private fun persist(profiles: List<VoiceCloneProfile>) {
        if (!folder.exists() && !folder.mkdirs()) throw IOException("Sem pasta privada para vozes.")
        val bytes = JSONObject().put("schema", 1).put("voices", JSONArray().apply {
            profiles.forEach { item -> put(JSONObject().put("id", item.id)
                .put("name", item.name).put("engine", item.engine)
                .put("createdAt", item.createdAt)) }
        }).toString().toByteArray(Charsets.UTF_8)
        val out = manifest.startWrite()
        try { out.write(bytes); manifest.finishWrite(out) }
        catch (e: Exception) { manifest.failWrite(out); throw e }
    }

    suspend fun add(context: Context, uri: Uri, label: String, engine: String): VoiceCloneProfile =
        withContext(Dispatchers.IO) {
            require(VoiceClonePolicy.engineAllowed(engine)) { "Motor de voz inválido." }
            val name = label.trim().take(60)
            require(name.isNotEmpty()) { "Informe o nome da voz." }
            require(uri.scheme == "content") { "Selecione um WAV pelo Android." }
            val profile = VoiceCloneProfile(UUID.randomUUID().toString(), name, engine,
                System.currentTimeMillis())
            val destination = reference(profile)
            val part = File(folder, "${profile.id}.part")
            if (!folder.exists() && !folder.mkdirs()) throw IOException("Sem pasta para referência.")
            try {
                (context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Referência não pôde ser aberta.")).use { source ->
                    part.outputStream().use { out ->
                        val buffer = ByteArray(8192)
                        var count = 0L
                        while (true) {
                            val n = source.read(buffer)
                            if (n < 0) break
                            count += n
                            if (count > VoiceClonePolicy.MAX_REFERENCE) throw IOException("Referência acima de 10 MiB.")
                            out.write(buffer, 0, n)
                        }
                        if (count < 128) throw IOException("Referência curta ou vazia.")
                        out.fd.sync()
                    }
                }
                part.inputStream().use { input ->
                    val header = ByteArray(12)
                    require(input.read(header) == 12 && String(header, 0, 4) == "RIFF" &&
                        String(header, 8, 4) == "WAVE") { "Escolha um áudio WAV válido." }
                }
                val extractor = MediaExtractor()
                val duration = try {
                    extractor.setDataSource(part.path)
                    val index = (0 until extractor.trackCount).firstOrNull {
                        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                    } ?: throw IOException("Nenhuma faixa de áudio encontrada.")
                    extractor.getTrackFormat(index).getLong(MediaFormat.KEY_DURATION) / 1_000_000L
                } finally { extractor.release() }
                require(duration in 3..30) { "Use uma referência de 3 a 30 segundos, sem música ou ruídos." }
                check(part.renameTo(destination)) { "Falha ao finalizar o WAV." }
                synchronized(this@VoiceCloneLibrary) {
                    val existing = load()
                    check(existing.size < 100) { "Limite de 100 vozes no aparelho." }
                    persist(existing + profile)
                }
                profile
            } catch (e: Exception) {
                destination.delete()
                throw e
            } finally { part.delete() }
        }

    @Synchronized fun remove(profile: VoiceCloneProfile) {
        val existing = load()
        require(existing.any { it.id == profile.id }) { "Voz não encontrada." }
        persist(existing.filterNot { it.id == profile.id })
        reference(profile).delete()
    }
}
