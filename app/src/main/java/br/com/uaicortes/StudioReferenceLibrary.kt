package br.com.uaicortes

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Biblioteca compartilhada entre histórias; não altera os projetos de vídeo nem o catálogo Flow. */
internal data class StudioLibraryData(
    val people: List<StudioPerson> = emptyList(),
    val places: List<StudioPlace> = emptyList(),
    val styles: List<String> = emptyList()
)

internal class StudioReferenceLibrary(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.filesDir, "flow_studio_library_v1").apply { mkdirs() }
    private val atomic = AtomicFile(File(directory, "library.json"))
    private val pictures = File(directory, "images").apply { mkdirs() }

    @Synchronized fun read(): StudioLibraryData {
        if (!atomic.baseFile.exists()) return StudioLibraryData()
        val bytes = atomic.openRead().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > 1024 * 1024) throw IOException("Biblioteca excede o limite de 1 MiB.")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        require(json.optInt("schema") == 1) { "Biblioteca de referências desconhecida." }
        val people = json.optJSONArray("people") ?: JSONArray()
        val places = json.optJSONArray("places") ?: JSONArray()
        val styles = json.optJSONArray("styles") ?: JSONArray()
        require(people.length() <= 100 && places.length() <= 100 && styles.length() <= 30)
        return StudioLibraryData(
            (0 until people.length()).map { StudioPerson.read(people.getJSONObject(it)) },
            (0 until places.length()).map { StudioPlace.read(places.getJSONObject(it)) },
            (0 until styles.length()).map { styles.optString(it).take(120) }.filter { it.isNotBlank() }
        )
    }

    @Synchronized fun save(next: StudioLibraryData) {
        read() // Bloqueia sobrescrita de um catálogo corrompido.
        require(next.people.size <= 100 && next.places.size <= 100 && next.styles.size <= 30)
        val json = JSONObject().put("schema", 1)
            .put("people", JSONArray().also { a -> next.people.forEach { a.put(it.json()) } })
            .put("places", JSONArray().also { a -> next.places.forEach { a.put(it.json()) } })
            .put("styles", JSONArray().also { a -> next.styles.forEach { a.put(it.take(120)) } })
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 1024 * 1024) { "Biblioteca cheia." }
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (exception: Exception) {
            stream?.let { atomic.failWrite(it) }
            throw exception
        }
    }

    fun asset(filename: String): File? {
        if (!filename.matches(Regex("[a-fA-F0-9-]{36}\\.(png|jpg|webp)"))) return null
        return File(pictures, filename).takeIf { it.isFile && it.length() in 1..12_000_000 }
    }

    /** Imagem opcional via seletor do Android; sem permissão permanente nem upload. */
    fun importImage(referenceId: String, uri: Uri): String {
        require(referenceId.matches(Regex("[a-fA-F0-9-]{36}")))
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > 12_000_000) throw IOException("Imagem maior que 12 MB.")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IOException("Não foi possível abrir a imagem.")
        val ext = when {
            bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() -> "jpg"
            bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4e.toByte() && bytes[3] == 0x47.toByte() -> "png"
            bytes.size >= 12 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WEBP" -> "webp"
            else -> throw IOException("Use JPEG, PNG ou WebP.")
        }
        val bitmapInfo = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bitmapInfo)
        require(bitmapInfo.outWidth in 1..10000 && bitmapInfo.outHeight in 1..15000) { "Imagem inválida." }
        val target = File(pictures, "$referenceId.$ext")
        val temporary = File.createTempFile("ref-", ".tmp", pictures)
        try {
            temporary.outputStream().use { it.write(bytes) }
            if (!temporary.renameTo(target)) throw IOException("Não foi possível salvar a imagem.")
            listOf("png", "jpg", "webp").filterNot { it == ext }.forEach { File(pictures, "$referenceId.$it").delete() }
        } finally { temporary.delete() }
        return target.name
    }
}

/** Sugestões locais imediatas: nunca gastam tokens nem dependem de rede. */
internal object StudioReferenceSuggestions {
    val characterKinds = listOf("👽 Alienígena", "🧑 Pessoa", "🐱 Animal", "🥑 Fruta", "🤖 Robô", "🪑 Objeto vivo")
    val places = listOf("🍳 Cozinha", "🌳 Praça", "🚀 Nave espacial", "🌲 Floresta", "🏠 Quarto", "🏙️ Cidade")

    fun person(kind: String): StudioPerson = when {
        "Alienígena" in kind -> StudioPerson(name = "Alienígena", appearance = "Ser extraterrestre de olhos expressivos e formato singular, traços consistentes em todas as cenas.", clothes = "Macacão espacial", voice = "Voz curiosa", personality = "Curioso e desconfiado", objects = "Dispositivo alienígena")
        "Pessoa" in kind -> StudioPerson(name = "Pessoa", appearance = "Pessoa com rosto e características reconhecíveis em todas as cenas.", clothes = "Roupa cotidiana", voice = "Voz natural em português brasileiro", personality = "Expressivo e espontâneo")
        "Animal" in kind -> StudioPerson(name = "Animal", appearance = "Animal com proporções características e expressões bem visíveis.", clothes = "Sem roupas, a menos que a história peça", personality = "Brincalhão")
        "Fruta" in kind -> StudioPerson(name = "Abacate", appearance = "Abacate antropomórfico, olhos grandes, braços e pernas curtos.", personality = "Rabugento e engraçado", voice = "Voz caricata em português brasileiro")
        "Robô" in kind -> StudioPerson(name = "Robô", appearance = "Robô compacto com olhos luminosos e movimentos mecânicos marcantes.", clothes = "Carcaça metálica", voice = "Voz digital expressiva", personality = "Lógico e atrapalhado")
        else -> StudioPerson(name = "Objeto animado", appearance = "Objeto cotidiano com rosto, braços e pernas, mantendo sua forma característica.", personality = "Teimoso e divertido")
    }

    fun place(kind: String): StudioPlace = when {
        "Cozinha" in kind -> StudioPlace("Cozinha", "Cozinha iluminada, balcão, armários e espaço para personagens.", "Copo e utensílios")
        "Praça" in kind -> StudioPlace("Praça", "Praça de cidade pequena, árvores, bancos e calçamento.", "Placas e bancos")
        "Nave" in kind -> StudioPlace("Nave espacial", "Interior de nave futurista, painéis e janelas para o espaço.", "Controles luminosos")
        "Floresta" in kind -> StudioPlace("Floresta", "Trilha rodeada de árvores, folhas e iluminação natural.", "Pedras e troncos")
        "Quarto" in kind -> StudioPlace("Quarto", "Quarto doméstico acolhedor com cama, janela e objetos pessoais.", "Luminária")
        else -> StudioPlace("Cidade", "Rua urbana com construções, calçadas e pontos de referência consistentes.", "Postes e veículos")
    }
}
