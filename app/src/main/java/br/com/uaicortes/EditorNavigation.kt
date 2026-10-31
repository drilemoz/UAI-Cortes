package br.com.uaicortes

/** Rotas e regras de recuperação em um único lugar para todas as ferramentas. */
internal object EditorNavigation {
    private val routes = setOf(
        "home", "projects", "settings", "general", "appearance", "about", "whats_new", "diagnostics", "speech_model",
        "editor", "media", "animate", "pip", "audio", "trim", "transition", "subtitles",
        "watermark", "export", "autosubtitle"
    )
    private val projectRoutes = setOf(
        "editor", "pip", "audio", "trim", "transition", "subtitles", "watermark",
        "export", "autosubtitle"
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
        "editor", "home", "projects", "settings" -> "home"
        "media" -> knownRoute(mediaOrigin)
        "animate" -> if (animationDestination == "append") "editor" else "home"
        "general", "appearance", "about", "diagnostics" -> "settings"
        "whats_new" -> if (previous == "settings" || previous == "about") previous else "home"
        "speech_model" -> if (previous == "autosubtitle") "autosubtitle" else "settings"
        "pip", "audio", "trim", "transition", "subtitles", "watermark", "export" -> "editor"
        "autosubtitle" -> "subtitles"
        else -> knownRoute(previous)
    }
}
