package br.com.uaicortes

/** Regras compartilhadas entre a criação de projetos e o nome do MP4. */
internal object ProjectNaming {
    private val invalid = Regex("""[\/:*?"<>|\p{Cntrl}]""")
    fun title(input: String): String = input.trim().replace(Regex("""\s+"""), " ").take(80)
    fun validTitle(input: String): Boolean = title(input).isNotEmpty()
    fun exportName(projectTitle: String): String {
        // Uma entrada 'video.mp4' vira 'video.mp4', jamais 'video.mp4.mp4'.
        val withoutExtension = title(projectTitle).replace(Regex("(?i)\\.mp4$"), "")
        val base = withoutExtension.replace(invalid, "_")
            .trim().trim('.', ' ').take(75)
        return (base.ifEmpty { "UaiCortes" }) + ".mp4"
    }
}
