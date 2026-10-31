package br.com.uaicortes

/** Regras compartilhadas entre a criação de projetos e o nome do MP4. */
internal object ProjectNaming {
    private val invalid = Regex("""[\/:*?"<>|\p{Cntrl}]""")
    fun title(input: String): String = input.trim().replace(Regex("""\s+"""), " ").take(80)
    fun validTitle(input: String): Boolean = title(input).isNotEmpty()
    fun exportName(projectTitle: String): String {
        val base = title(projectTitle).replace(invalid, "_")
            .trim().trim('.', ' ').take(75)
        return (base.ifEmpty { "UaiCortes" }) + ".mp4"
    }
}
