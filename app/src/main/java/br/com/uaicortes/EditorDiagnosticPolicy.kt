package br.com.uaicortes

internal object EditorDiagnosticPolicy {
    enum class Target(val code: String) {
        MAIN_IMAGE("main_image"), PIP_IMAGE("pip_image"), PIP_VIDEO("pip_video"),
        PIP_MEDIA("pip_media");

        companion object {
            fun pip(isImage: Boolean) = if (isImage) PIP_IMAGE else PIP_VIDEO
        }
    }

    enum class MediaStage(val code: String) {
        IMPORT_READ("import_read"), IMPORT_COPY("import_copy"), IMPORT_READY("import_ready"),
        IMPORT_BLOCKED("import_blocked"), IMPORT_COMMIT("import_commit"),
        PREVIEW_OPEN("preview_open"), PREVIEW_DECODE("preview_decode"),
        PREVIEW_PLAYBACK("preview_playback"), EXPORT_READ("export_read")
    }

    enum class Result(val code: String) {
        READY("ready"), FAILED("failed"), REJECTED("rejected"), BUSY("busy"),
        NO_DURATION("no_duration"), NO_SPACE_ON_TIMELINE("no_space_on_timeline"),
        LIMIT("limit"), NOT_FOUND("not_found"), PERMISSION("permission"), INVALID_IMAGE("invalid_image")
    }

    enum class TransitionStage(val code: String) {
        PLAN_FAILED("plan_failed"), FRAME_PREP_FAILED("frame_prep_failed"),
        RENDER_START("render_start"), RENDER_READY("render_ready"), RENDER_FAILED("render_failed"),
        CANCELLED("cancelled"), PLAY_FAILED("play_failed"), FIRST_FRAME("first_frame"),
        RENDER_TIMEOUT("render_timeout"), PLAY_TIMEOUT("play_timeout")
    }

    fun media(target: Target, stage: MediaStage, result: Result, playerCode: Int? = null) =
        "target=${target.code} stage=${stage.code} result=${result.code}" +
            playerCode?.let { " media3=${it.coerceIn(0, 99_999)}" }.orEmpty()

    fun transition(preview: Boolean, stage: TransitionStage, boundary: Int? = null,
                   kind: String? = null, playerCode: Int? = null) =
        "mode=${if (preview) "preview" else "export"} stage=${stage.code}" +
            boundary?.takeIf { it >= 0 }?.let { " junction=${it.toLong() + 1L}" }.orEmpty() +
            kind?.let { " effect=${effect(it)}" }.orEmpty() +
            playerCode?.let { " media3=${it.coerceIn(0, 99_999)}" }.orEmpty()

    private fun effect(kind: String): String = when (kind) {
        TransitionEffects.CUT -> "cut"
        TransitionEffects.BLACK -> "fade_black"
        JunctionTransitionPlan.DISSOLVE -> "dissolve"
        JunctionTransitionPlan.PUSH_LEFT -> "push_left"
        JunctionTransitionPlan.PUSH_RIGHT -> "push_right"
        else -> if (TransitionEffects.supported(kind)) "legacy" else "unknown"
    }
}

internal data class TransitionDiagnosticContext(val boundary: Int, val kind: String)
