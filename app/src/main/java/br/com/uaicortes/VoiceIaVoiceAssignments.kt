package br.com.uaicortes

import org.json.JSONObject

/** Vozes escolhidas pelo usuário para um projeto. IDs de locutor/legenda, nunca nomes reconhecidos. */
data class VoiceIaVoiceAssignments(
    val defaultVoiceId: String? = null,
    val speakerVoices: Map<String, String> = emptyMap(),
    val cueVoices: Map<String, String> = emptyMap(),
    val defaultStyle: String? = null,
    val speakerStyles: Map<String, String> = emptyMap(),
    val cueStyles: Map<String, String> = emptyMap()
) {
    fun json(): JSONObject = JSONObject()
        .put("defaultVoiceId", defaultVoiceId)
        .put("speakerVoices", JSONObject().also { out ->
            speakerVoices.entries.take(SubtitleSpeakerPolicy.MAX_SPEAKERS).forEach { (id, voice) ->
                if (VoiceIaSpeakerVoicePolicy.validSpeakerId(id) && VoiceIaSpeakerVoicePolicy.validVoice(voice))
                    out.put(id, voice)
            }
        })
        .put("cueVoices", JSONObject().also { out ->
            cueVoices.entries.take(VoiceIaSpeakerVoicePolicy.MAX_CUE_OVERRIDES).forEach { (id, voice) ->
                if (VoiceIaSpeakerVoicePolicy.validCueId(id) && VoiceIaSpeakerVoicePolicy.validVoice(voice))
                    out.put(id, voice)
            }
        })
        .put("defaultStyle", defaultStyle?.takeIf(VoiceIaStylePolicy::valid))
        .put("speakerStyles", JSONObject().also { out ->
            speakerStyles.entries.take(SubtitleSpeakerPolicy.MAX_SPEAKERS).forEach { (id, style) ->
                if (VoiceIaSpeakerVoicePolicy.validSpeakerId(id) && VoiceIaStylePolicy.valid(style))
                    out.put(id, style)
            }
        })
        .put("cueStyles", JSONObject().also { out ->
            cueStyles.entries.take(VoiceIaSpeakerVoicePolicy.MAX_CUE_OVERRIDES).forEach { (id, style) ->
                if (VoiceIaSpeakerVoicePolicy.validCueId(id) && VoiceIaStylePolicy.valid(style))
                    out.put(id, style)
            }
        })

    companion object {
        fun read(json: JSONObject?): VoiceIaVoiceAssignments {
            if (json == null) return VoiceIaVoiceAssignments()
            fun readMap(key: String, limit: Int, validId: (String) -> Boolean): Map<String, String> {
                val objectValue = json.optJSONObject(key) ?: return emptyMap()
                val result = linkedMapOf<String, String>()
                val keys = objectValue.keys()
                while (keys.hasNext() && result.size < limit) {
                    val id = keys.next()
                    val voice = objectValue.optString(id)
                    if (validId(id) && VoiceIaSpeakerVoicePolicy.validVoice(voice)) result[id] = voice
                }
                return result
            }
            fun readStyles(key: String, limit: Int, validId: (String) -> Boolean): Map<String, String> {
                val obj = json.optJSONObject(key) ?: return emptyMap()
                val styles = linkedMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext() && styles.size < limit) {
                    val id = keys.next()
                    val style = obj.optString(id)
                    if (validId(id) && VoiceIaStylePolicy.valid(style)) styles[id] = style
                }
                return styles
            }
            return VoiceIaVoiceAssignments(
                json.optString("defaultVoiceId").takeIf(VoiceIaSpeakerVoicePolicy::validVoice),
                readMap("speakerVoices", SubtitleSpeakerPolicy.MAX_SPEAKERS,
                    VoiceIaSpeakerVoicePolicy::validSpeakerId),
                readMap("cueVoices", VoiceIaSpeakerVoicePolicy.MAX_CUE_OVERRIDES,
                    VoiceIaSpeakerVoicePolicy::validCueId),
                json.optString("defaultStyle").takeIf(VoiceIaStylePolicy::valid),
                readStyles("speakerStyles", SubtitleSpeakerPolicy.MAX_SPEAKERS,
                    VoiceIaSpeakerVoicePolicy::validSpeakerId),
                readStyles("cueStyles", VoiceIaSpeakerVoicePolicy.MAX_CUE_OVERRIDES,
                    VoiceIaSpeakerVoicePolicy::validCueId)
            )
        }
    }
}

/** Precedência: fala específica > personagem conhecido > voz geral > legado. Nunca chama IA. */
internal object VoiceIaSpeakerVoicePolicy {
    const val MAX_CUE_OVERRIDES = 1000
    fun validVoice(id: String?): Boolean = id != null && id.isNotBlank() && !id.equals("null", true) &&
        id.length <= 160 &&
        id.none { it.isISOControl() }
    fun validSpeakerId(id: String): Boolean = id.isNotBlank() && id.length <= 80
    fun validCueId(id: String): Boolean = id.isNotBlank() && id.length <= 160

    fun preferred(cue: SubtitleCue, speakers: List<SubtitleSpeaker>,
        saved: VoiceIaVoiceAssignments): String? {
        saved.cueVoices[cue.id]?.let { if (validVoice(it)) return it }
        val speaker = cue.speakerId?.takeIf { id -> speakers.any { it.id == id } }
        speaker?.let { saved.speakerVoices[it] }?.let { if (validVoice(it)) return it }
        return saved.defaultVoiceId?.takeIf(::validVoice)
    }

    fun speakerVoice(project: VideoProject, id: String, voice: String?): VideoProject? {
        if (project.subtitleSpeakers.none { it.id == id } ||
            (voice != null && !validVoice(voice))) return null
        val updated = project.voiceIaVoices.speakerVoices.toMutableMap().apply {
            if (voice == null) remove(id) else put(id, voice)
        }
        return project.copy(voiceIaVoices = project.voiceIaVoices.copy(speakerVoices = updated))
    }

    fun cueVoice(project: VideoProject, id: String, voice: String?): VideoProject? {
        if (project.subtitles.none { it.id == id } ||
            (voice != null && !validVoice(voice))) return null
        val updated = project.voiceIaVoices.cueVoices.toMutableMap().apply {
            if (voice == null) remove(id) else put(id, voice)
        }
        if (updated.size > MAX_CUE_OVERRIDES) return null
        return project.copy(voiceIaVoices = project.voiceIaVoices.copy(cueVoices = updated))
    }

    /** Aplicação geral explícita substitui configurações anteriores, sem alterar WAVs/legendas. */
    fun all(project: VideoProject, voice: String): VideoProject? =
        voice.takeIf(::validVoice)?.let { project.copy(voiceIaVoices = project.voiceIaVoices.copy(
            defaultVoiceId = it, speakerVoices = emptyMap(), cueVoices = emptyMap())) }
}
