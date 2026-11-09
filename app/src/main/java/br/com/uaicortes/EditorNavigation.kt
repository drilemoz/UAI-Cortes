package br.com.uaicortes

/** Rotas e regras de recuperação em um único lugar para todas as ferramentas. */
internal object EditorNavigation {
    private val routes = setOf(
        "home", "quick_split", "quick_audio", "edit_audio", "projects", "social", "social_publish", "api_center", "settings", "gemini_settings", "voice_center", "text_to_voice", "general", "backup_settings", "appearance", "about", "whats_new", "diagnostics", "speech_model",
        "editor", "media_repair", "media", "animate", "flow_workspace", "pip", "audio", "voices", "clip_audio", "trim", "transition", "subtitles",
        "watermark", "publication", "flow_prompt", "export", "autosubtitle"
    )
    private val projectRoutes = setOf(
        "editor", "media_repair", "pip", "audio", "voices", "clip_audio", "trim", "transition", "subtitles", "watermark",
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
        "editor", "home", "quick_split", "quick_audio", "edit_audio", "projects", "social" -> "home"
        "social_publish" -> "social"
        "api_center" -> if (previous == "social") "social" else "settings"
        "settings" -> if (previous == "audio" || previous == "voices") previous else "home"
        "media" -> knownRoute(mediaOrigin)
        "flow_workspace" -> "home"
        "voice_center" -> if (previous == "settings") "settings" else "home"
        "text_to_voice" -> "home"
        "animate" -> if (animationDestination == "append") "editor" else "home"
        "gemini_settings" -> if (previous == "audio" || previous == "voices" || previous == "api_center" || previous == "text_to_voice") previous else "settings"
        "general", "backup_settings", "appearance", "about", "diagnostics" -> "settings"
        "whats_new" -> if (previous == "settings" || previous == "about") previous else "home"
        "speech_model" -> if (previous == "autosubtitle") "autosubtitle" else "settings"
        "media_repair", "pip", "audio", "voices", "clip_audio", "trim", "transition", "subtitles", "watermark", "publication", "flow_prompt", "export" -> "editor"
        "autosubtitle" -> "subtitles"
        else -> knownRoute(previous)
    }
}
