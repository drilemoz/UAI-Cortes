package br.com.uaicortes

/** Um cartão = uma geração no Flow. Não obriga gerar/aprovar história antes. */
internal object FlowStudioClipPolicy {
    fun canDivide(story: StudioStory): Boolean = story.idea.isNotBlank() ||
        story.originalScript.isNotBlank() || story.ready()

    fun canPrepare(scene: StudioScene): Boolean = scene.actions.isNotBlank() ||
        scene.visual.isNotBlank() || scene.effectiveDialogue().isNotBlank()

    fun prepare(scene: StudioScene, draft: FlowStudioDraft, index: Int): StudioScene {
        require(canPrepare(scene)) { "Descreva a ação, fala ou reação antes de preparar o prompt." }
        return scene.copy(prompt = StudioPromptPolicy.compose(scene, draft, draft.scenes.getOrNull(index - 1)))
    }
}
