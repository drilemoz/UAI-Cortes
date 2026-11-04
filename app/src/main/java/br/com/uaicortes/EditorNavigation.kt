package br.com.uaicortes

/** Rotas e regras de recuperação em um único lugar para todas as ferramentas. */
internal object EditorNavigation {
    private val routes = setOf(
        "home", "projects", "settings", "gemini_settings", "general", "backup_settings", "appearance", "about", "whats_new", "diagnostics", "speech_model",
        "editor", "media", "animate", "flow_workspace", "pip", "audio", "voices", "clip_audio", "trim", "transition", "subtitles",
        "watermark", "publication", "flow_prompt", "export", "autosubtitle"
    )
    private val projectRoutes = setOf(
        "editor", "pip", "audio", "voices", "clip_audio", "trim", "transition", "subtitles", "watermark",
        "publication", "flow_prompt", "export", "autosubtitle"
    )

    fun knownRoute(value: String): String = value.takeIf { it in routes } ?: "home"

    fun restoreRoute(route: String, hasProject: Boolean, mediaOrigin: String, mediaAction: String): String {
        val validRoute = knownRoute(route)
        if (!hasProject && validRoute in projectRoutes) return "home"
        if (!hasProject && validRoute == "media" &&
            !(mediaAction == "new" || (mediaAction == "animate_images" && mediaOrigin == "animate"))) return "home"
        return validRoute
    }

    fun back(route: String, previous: String, mediaOrigin: String, animationDestination: String): String = when (route) {
        "editor", "home", "projects" -> "home"
        "settings" -> if (previous == "audio" || previous == "voices") previous else "home"
        "media" -> knownRoute(mediaOrigin)
        "flow_workspace" -> "home"
        "animate" -> "home" // Voltar sempre leva à tela inicial; adicionar ao editor é outra ação.
        "gemini_settings" -> if (previous == "audio" || previous == "voices") previous else "settings"
        "general", "backup_settings", "appearance", "about", "diagnostics" -> "settings"
        "whats_new" -> if (previous == "settings" || previous == "about") previous else "home"
        "speech_model" -> if (previous == "autosubtitle") "autosubtitle" else "settings"
        "pip", "audio", "voices", "clip_audio", "trim", "transition", "subtitles", "watermark", "publication", "flow_prompt", "export" -> "editor"
        "autosubtitle" -> "subtitles"
        else -> knownRoute(previous)
    }
}
