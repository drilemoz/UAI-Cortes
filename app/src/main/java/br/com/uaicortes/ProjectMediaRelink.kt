package br.com.uaicortes


/** Identifica cada uso de arquivo por ID, nunca por posição na lista ou nome de arquivo. */
internal data class MediaRelinkEntry(
    val kind: String,
    val id: String,
    val originalUri: String,
    val title: String,
    val mimePrefix: String,
    val minimumSourceEndMs: Long = 0L
) {
    val key: String get() = "$kind:$id"
}

/** Não modifica horários, cortes, IDs, posições ou propriedades de exportação. */
internal object ProjectMediaRelink {
    fun entries(project: VideoProject): List<MediaRelinkEntry> = buildList {
        project.clips.forEach { clip ->
            add(MediaRelinkEntry("clip", clip.id, clip.uri, clip.title,
                if (clip.isImage) "image/" else "video/",
                if (clip.isImage) 0L else clip.endMs))
        }
        project.overlays.forEach { pip ->
            add(MediaRelinkEntry("pip", pip.id, pip.uri,
                if (pip.isImage) "Imagem PiP" else "Vídeo PiP",
                if (pip.isImage) "image/" else "video/",
                if (pip.isImage) 0L else pip.sourceStartMs + pip.durationMs))
        }
        project.music?.let { music ->
            add(MediaRelinkEntry("music", "music", music.uri,
                music.title.ifBlank { "Música" }, "audio/"))
        }
        project.audioTracks.forEach { audio ->
            add(MediaRelinkEntry("audio", audio.id, audio.uri, audio.title,
                "audio/", audio.sourceStartMs + audio.durationMs))
        }
    }

    /** Proteção contra resultado atrasado de picker e troca de projeto/arquivo. */
    fun replace(project: VideoProject, target: MediaRelinkEntry, newUri: String): VideoProject {
        require(newUri.startsWith("content://")) { "Selecione um arquivo pelo gerenciador Android." }
        require(entries(project).any { it.key == target.key && it.originalUri == target.originalUri }) {
            "A mídia ou o projeto mudou durante a seleção. Abra a recuperação novamente."
        }
        return when (target.kind) {
            "clip" -> project.copy(clips = project.clips.map {
                if (it.id == target.id) it.copy(uri = newUri) else it
            })
            "pip" -> project.copy(overlays = project.overlays.map {
                if (it.id == target.id) it.copy(uri = newUri) else it
            })
            "music" -> project.copy(music = project.music?.copy(uri = newUri))
            "audio" -> project.copy(audioTracks = project.audioTracks.map {
                if (it.id == target.id) it.copy(uri = newUri) else it
            })
            else -> error("Tipo de mídia desconhecido")
        }
    }
}

