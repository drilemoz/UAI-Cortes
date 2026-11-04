package br.com.uaicortes

/** Política pura de segurança de cotas por projeto. */
internal object GeminiFailoverPolicy {
    fun canSwitch(code: Int, enabled: Boolean, originProject: String, nextProject: String,
                  triedProjects: Set<String>, isPaused: Boolean): Boolean =
        code == 429 && enabled && originProject.isNotBlank() && nextProject.isNotBlank() &&
            originProject != nextProject && nextProject !in triedProjects && !isPaused
}
