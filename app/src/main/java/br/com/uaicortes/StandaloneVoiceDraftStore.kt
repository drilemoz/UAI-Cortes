package br.com.uaicortes

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.Properties

/** Rascunho explicitamente salvo. Texto não é enviado à rede nem incluído em diagnósticos. */
internal class StandaloneVoiceDraftStore(context: Context, batch: Boolean = false) {
    private val root = context.applicationContext.filesDir
    private val target = File(root, if (batch) "standalone_voice_batch_v1.properties" else "standalone_voice_draft_v1.properties")
    internal data class Snapshot(val text: String, val model: String, val voice: GeminiVoice?,
        val style: String, val language: String, val mode: VoiceStudioMode = VoiceStudioMode.CONTINUOUS,
        val tonePreset: String = "", val windows: List<Long> = emptyList())

    fun exists(): Boolean = target.isFile
    fun save(draft: Snapshot): Boolean {
        if (draft.text.length > StandaloneVoiceTextPolicy.MAX_CHARS ||
            StandaloneVoiceTextPolicy.length(draft.text) > StandaloneVoiceTextPolicy.MAX_CHARS) return false
        val p = Properties().apply {
            setProperty("schema", "1"); setProperty("text", draft.text)
            setProperty("model", draft.model); setProperty("style", draft.style)
            setProperty("language", draft.language)
            setProperty("mode", draft.mode.name)
            setProperty("tone", draft.tonePreset)
            setProperty("windows", draft.windows.joinToString(","))
            setProperty("voiceId", draft.voice?.id.orEmpty())
            setProperty("voiceName", draft.voice?.name.orEmpty())
        }
        val part = File(root, "${target.name}.part")
        return try {
            part.outputStream().use { p.store(it, "UaiCortes local draft"); it.fd.sync() }
            if (!part.renameTo(target)) throw IOException("Não foi possível concluir o rascunho.")
            true
        } catch (_: Exception) { false }
        finally { part.delete() }
    }

    fun load(): Snapshot? {
        if (!target.isFile || target.length() !in 1..32768) return null
        return runCatching {
            val p = Properties()
            target.inputStream().use { p.load(it) }
            if (p.getProperty("schema") != "1") return@runCatching null
            val text = p.getProperty("text", "")
            if (text.length > StandaloneVoiceTextPolicy.MAX_CHARS ||
                StandaloneVoiceTextPolicy.length(text) > StandaloneVoiceTextPolicy.MAX_CHARS) return@runCatching null
            val model = p.getProperty("model").orEmpty().takeIf { it in GeminiModels.supported } ?: GeminiModels.LITE
            val style = p.getProperty("style").orEmpty().takeIf { it in (GeminiNarrationStyling.presets.map { x -> x.label } + VoiceStudioPlan.AUTO) } ?: "Neutra"
            val language = p.getProperty("language").orEmpty().takeIf { it in GeminiSpeechLanguage.choices }
                ?: GeminiSpeechLanguage.PORTUGUESE_BR
            val id = p.getProperty("voiceId", "").take(150)
            val voice = id.takeIf { it.isNotBlank() && GeminiVoiceFavorites.valid(it) }
                ?.let { GeminiVoice(it, p.getProperty("voiceName", it).take(120), "") }
            val mode = VoiceStudioMode.entries.firstOrNull { it.name == p.getProperty("mode") }
                ?: VoiceStudioMode.CONTINUOUS
            val windows = p.getProperty("windows", "").split(',').mapNotNull { it.toLongOrNull()?.takeIf { n -> n > 0L } }
                .take(VoiceStudioPlan.MAX_SCENES)
            Snapshot(text, model, voice, style, language, mode, p.getProperty("tone", ""), windows)
        }.getOrNull()
    }
    fun clear(): Boolean = !target.exists() || target.delete()
}
