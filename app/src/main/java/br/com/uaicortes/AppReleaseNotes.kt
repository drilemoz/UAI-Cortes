package br.com.uaicortes

import java.security.MessageDigest

/** Fonte única: a tela Sobre e a tela Novidades exibem exatamente estas entradas. */
internal data class ReleaseFeature(
    val key: String,
    val title: String,
    val description: String,
    val symbol: String
)

internal data class ReleaseSummary(
    val version: String,
    val features: List<ReleaseFeature>
)

internal object AppReleaseNotes {
    const val VERSION_NAME = "0.1.86"
    const val VERSION_CODE = 87

    /** Atualizar a entrada mais recente quando houver uma novidade publicável. */
    val recent: List<ReleaseSummary> = listOf(
        ReleaseSummary("0.1.86", listOf(
            ReleaseFeature("ci72-media3-subtitle-optin", "Correção do Lint das legendas",
                "Contorno e sombra agora declaram o uso controlado da API Media3, sem alterar a aparência e sem desativar o Lint.", "check")
        )),
        ReleaseSummary("0.1.85", listOf(
            ReleaseFeature("ci71-whatsnew-singleton-test", "Correção do teste de novidades",
                "Ajustado o teste de ordem das novidades para versões com apenas uma mudança, sem modificar a regra de exibição.", "check")
        )),
        ReleaseSummary("0.1.84", listOf(
            ReleaseFeature("ci70-kotlin-visibility", "Exportação compatível com o compilador Kotlin",
                "Corrigida a visibilidade da função de início de exportação que impedia a compilação Debug e Release no workflow 70.", "check")
        )),
        ReleaseSummary("0.1.83", listOf(
            ReleaseFeature("export-storage-review", "Estimativa de espaço para projetos longos",
                "Corrigido o limite de 24 horas e prevenido transbordamento em reservas e estimativas de exportação.", "film"),
            ReleaseFeature("export-storage-message", "Avisos de espaço específicos",
                "O aviso de falta de armazenamento agora identifica o perfil escolhido e sugere reduzir a configuração.", "check")
        )),
        ReleaseSummary("0.1.82", listOf(
            ReleaseFeature("export-bitrate-presets", "Três perfis de qualidade do MP4",
                "Escolha Compacta, Alta ou Máxima em 720p/1080p, com estimativa de espaço específica para cada perfil.", "film"),
            ReleaseFeature("export-gop-request", "Quadros-chave solicitados a cada 2 segundos",
                "A exportação H.264 solicita GOP de 2 segundos, mantendo fallback para codecs que ajustam parâmetros.", "film"),
            ReleaseFeature("export-target-versus-actual", "Comparação de bitrate solicitado e medido",
                "O relatório diferencia a taxa selecionada da taxa real informada pelo Media3 e avisa quando houver grande diferença.", "check"),
            ReleaseFeature("export-source-upscale", "Aviso sobre nitidez da origem",
                "A exportação avisa quando algum clipe precisará ser ampliado e estima o tamanho do projeto na qualidade escolhida.", "film")
        )),
        ReleaseSummary("0.1.81", listOf(
            ReleaseFeature("export-text-resolution", "Legendas renderizadas após dimensionamento",
                "Vídeos verticais passam a usar 1080×1920 em 1080p; dimensionamento ocorre antes de renderizar legendas e marca d'água.", "captions"),
            ReleaseFeature("export-measurements", "Diagnóstico real do MP4",
                "Exibe resolução, bitrate, quadros e tamanho informados pelo Media3 após salvar; distingue resolução pedida da obtida.", "film")
        )),
        ReleaseSummary("0.1.80", listOf(
            ReleaseFeature("subtitle-word-wrap-guard", "Legendas sem cortes em palavras extensas",
                "Ajustada a redução da fonte para respeitar a largura do quadro mesmo com palavras excepcionalmente longas, na prévia e exportação.", "captions"),
            ReleaseFeature("subtitle-ci-fix", "Correção dos testes do exportador",
                "Corrigida a regressão identificada no workflow Gerar APK 67; versões, metadados e testes sincronizados.", "check")
        )),
        ReleaseSummary("0.1.79", listOf(
            ReleaseFeature("subtitle-bottom", "Legendas mais próximas do rodapé",
                "Novas legendas usam margem inferior de 8%; posição pode ser ajustada individualmente ou em lote.", "captions"),
            ReleaseFeature("subtitle-long-fit", "Ajuste automático de textos longos",
                "Reduz moderadamente a fonte de legendas extensas na prévia e exportação, sem cortar palavras.", "captions"),
            ReleaseFeature("pip-opacity", "Transparência de cada PiP",
                "Controle de opacidade independente para cada PiP, com persistência e exportação no Media3.", "film"),
            ReleaseFeature("capture-pauses", "Captura de quadro com pausa automática",
                "Pausa a reprodução no instante selecionado e mostra a posição durante o salvamento PNG.", "film")
        )),
        ReleaseSummary("0.1.78", listOf(
            ReleaseFeature("subtitle-save-editor", "Salvar legendas volta ao editor",
                "Salvar mantém o projeto ativo e retorna à edição; falha na gravação impede a navegação.", "check"),
            ReleaseFeature("subtitle-batch", "Editar todas as legendas",
                "Configure Estilo e Animação em abas, aplique em lote com confirmação e desfaça o último lote.", "captions"),
            ReleaseFeature("subtitle-progress", "Transcrição offline com progresso real",
                "Veja etapa, clipe, avanço baseado no áudio decodificado e tempo decorrido da operação.", "speed")
        )),
        ReleaseSummary("0.1.77", listOf(
            ReleaseFeature("gemini-menu", "Gemini organizado nas Configurações",
                "Agora a chave e as seis ações ficam em uma página própria, acessada pelo menu das Configurações.", "check"),
            ReleaseFeature("gemini-popup", "Narração com IA em janela compacta",
                "Gerador em pop-up com seletor pesquisável de vozes por nome e código real da API.", "music")
        )),
        ReleaseSummary("0.1.76", listOf(
            ReleaseFeature("gemini-tts", "Narração por IA com Gemini TTS",
                "Configure sua chave pessoal e gere áudio com vozes e estilos via API oficial, com salvamento e uso na timeline.", "music"),
            ReleaseFeature("gemini-protegido", "Configurações Gemini protegidas",
                "Importe, proteja, teste e remova sua chave com avisos de privacidade e cota.", "check")
        )),
        ReleaseSummary("0.1.75", listOf(
            ReleaseFeature("audio-clip-vinculo", "Narração somente no clipe escolhido",
                "Importe áudio, ouça, ajuste o volume e aplique apenas ao trecho selecionado.", "music"),
            ReleaseFeature("audio-clip-sincronismo", "Narração acompanha edições do clipe",
                "Áudio vinculado acompanha a ordem, cortes, divisão e duplicação do vídeo.", "music")
        )),
        ReleaseSummary("0.1.74", listOf(
            ReleaseFeature("audio-layout-cards", "Áudio organizado em três áreas",
                "Mixagem, adição de áudio e extração agora têm cartões próprios, com ações mais compactas.", "music"),
            ReleaseFeature("audio-volumes-tracks", "Gerenciamento de faixas mais claro",
                "Volumes, corte e exclusão permanecem disponíveis, com nomes de arquivo mais legíveis.", "music")
        )),
        ReleaseSummary("0.1.73", listOf(
            ReleaseFeature("velocidade-menos-mais", "Velocidade com botões − e +",
                "Controle a prévia de 0,1× a 6×, toque no número para escolher e restaure 1× rapidamente.", "speed"),
            ReleaseFeature("passo-quadro", "Quadro anterior e seguinte",
                "Dois botões substituem o ajuste fino antigo, pausando e avançando aproximadamente um quadro.", "film")
        )),
        ReleaseSummary("0.1.72", listOf(
            ReleaseFeature("velocidade-previa-5x", "Mais velocidades na prévia",
                "Reproduza de 0,25× até 5× com um seletor compacto, sem acelerar o vídeo exportado.", "speed"),
            ReleaseFeature("quadro-png-galeria", "Salvar quadro do vídeo",
                "Toque na câmera do editor para salvar uma imagem PNG do quadro original, no tempo escolhido.", "film")
        )),
        ReleaseSummary("0.1.71", listOf(
            ReleaseFeature("vosk-etapa-concluida", "Status de instalação corrigido",
                "Após instalar o modelo Vosk, a tela não fica mais mostrando descompactação em andamento.", "captions"),
            ReleaseFeature("vosk-teste-transcricao", "Verificação orientada",
                "Depois da instalação, o aplicativo orienta a testar uma fala curta nas legendas automáticas.", "check")
        )),
        ReleaseSummary("0.1.70", listOf(
            ReleaseFeature("vosk-pt-legado", "Modelo de português corrigido",
                "Instala o ZIP oficial de português do Vosk nos formatos antigo e moderno, sem rejeitar arquivos válidos.", "captions"),
            ReleaseFeature("vosk-botoes-linha", "Instalação mais compacta",
                "As opções de baixar no app, abrir no navegador e importar ZIP ficam na mesma linha.", "download")
        )),
        ReleaseSummary("0.1.69", listOf(
            ReleaseFeature("vosk-zip-manual", "Legendas offline sem depender do download interno",
                "Baixe o modelo no navegador e importe o ZIP diretamente no aplicativo.", "captions"),
            ReleaseFeature("vosk-progresso-recebido", "Download com diagnóstico",
                "Confira bytes recebidos, porcentagem quando disponível e mensagens de conexão mais claras.", "download")
        )),
        ReleaseSummary("0.1.68", listOf(
            ReleaseFeature("novidades-unificadas", "Novidades em destaque",
                "Uma tela especial apresenta as mudanças apenas uma vez. Você pode revisitá-la em Configurações ou Sobre.", "sparkles"),
            ReleaseFeature("notas-sincronizadas", "Informações sempre iguais",
                "A seção Sobre e a tela Novidades compartilham o mesmo catálogo de atualizações.", "sync")
        )),
        ReleaseSummary("0.1.67", listOf(
            ReleaseFeature("projetos-nomes", "Projetos mais organizados",
                "Exclusão simplificada e nomes personalizados usados como sugestão ao exportar.", "folder"),
            ReleaseFeature("galeria-compacta", "Galeria mais limpa",
                "Pesquisa discreta e menos elementos repetidos na seleção de vídeos.", "film"),
            ReleaseFeature("pip-tempos", "PiP com atalhos de tempo",
                "Aplique uma sobreposição a um clipe ou ao projeto inteiro usando campos compactos.", "layers"),
            ReleaseFeature("vosk-opcoes", "Legendas offline nas Configurações",
                "Gerencie o modelo de voz e acompanhe as etapas do download.", "captions")
        ))
    )

    val current: ReleaseSummary get() = recent.first()

    fun forAbout(): List<ReleaseSummary> = recent
    fun forWhatsNew(): List<ReleaseSummary> = recent

    /** Não usa versionCode: recompilar sem mudanças não deve reapresentar a tela. */
    fun fingerprintFor(features: List<ReleaseFeature>): String {
        val content = features.joinToString("\u001e") {
            listOf(it.key, it.title, it.description, it.symbol).joinToString("\u001f")
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    val currentFingerprint: String get() = fingerprintFor(current.features)
}

/** Regra testável sem Android: cada conjunto de novidades é mostrado uma única vez. */
internal object WhatsNewPolicy {
    fun shouldOpen(seen: Set<String>, fingerprint: String): Boolean =
        fingerprint.isNotBlank() && fingerprint !in seen

    fun acknowledged(seen: Set<String>, fingerprint: String): Set<String> =
        if (fingerprint.isBlank()) seen else seen + fingerprint
}

