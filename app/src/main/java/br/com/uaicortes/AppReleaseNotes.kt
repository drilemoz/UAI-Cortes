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
    const val VERSION_NAME = "0.1.161"
    const val VERSION_CODE = 162

    /** Atualizar a entrada mais recente quando houver uma novidade publicável. */
    val recent: List<ReleaseSummary> = listOf(
        ReleaseSummary("0.1.161", listOf(
            ReleaseFeature("flow-studio-161", "Estúdio de Cenas em três etapas",
                "Histórias, cenas e exportação ZIP com rascunhos locais, referências e modo manual preservado.", "check"),
            ReleaseFeature("flow-image-161", "Imagens iniciais com Gemini",
                "Consulta de modelos da chave, geração sob confirmação, prévia e compartilhamento SAF.", "captions"),
            ReleaseFeature("flow-continuity-161", "Continuidade e arquivos Flow",
                "Fichas de personagens, importação de quadros e associação manual de vídeos às cenas.", "check")
        )),
        ReleaseSummary("0.1.160", listOf(
            ReleaseFeature("gemini-ui-compact-160", "Painel Gemini mais compacto",
                "Resumo de consumo simplificado; ajuda técnica sob demanda e métricas detalhadas recolhidas.", "check"),
            ReleaseFeature("gemini-price-catalog-160", "Preços públicos como referência",
                "Sugere tarifas Standard de modelos conhecidos com datas e conferência; nunca lê cobrança oficial.", "captions"),
            ReleaseFeature("gemini-model-config-160", "Configuração unificada de modelos",
                "Cotas e preços numa só janela; plano gratuito depende de confirmação explícita.", "check")
        )),
        ReleaseSummary("0.1.159", listOf(
            ReleaseFeature("flow-batch-scene-159", "Batch de cenas no Flow",
                "Seleciona de 2 a 8 cenas e envia texto para refinamento assíncrono após confirmação e aviso de custo.", "check"),
            ReleaseFeature("flow-batch-safe-159", "Revisar resultados por cena",
                "Consulta jobs manualmente e aplica sugestões somente após revisão, recusando cenas alteradas.", "captions"),
            ReleaseFeature("flow-batch-rest-159", "Recuperação inline da API Batch",
                "Corrige leitura das respostas REST e preserva vínculo dos jobs com projeto e cenas sem armazenar prompts.", "check")
        )),
        ReleaseSummary("0.1.158", listOf(
            ReleaseFeature("gemini-batch-text-158", "Lotes Gemini de texto opcionais",
                "Permite enviar, após confirmação, pequenos jobs de texto Batch com modelo compatível e plano pago.", "check"),
            ReleaseFeature("gemini-batch-recovery-158", "Consultar e recuperar jobs",
                "Grava somente identificadores, consulta estado manualmente e recupera resultados inline sem nova geração.", "captions"),
            ReleaseFeature("gemini-batch-safety-158", "Proteção contra envio duplicado",
                "Registra envio incerto antes da chamada, não repete automaticamente nem troca de projeto silenciosamente.", "check")
        )),
        ReleaseSummary("0.1.157", listOf(
            ReleaseFeature("gemini-usage-latency-157", "Tempo real das operações Gemini",
                "Narrações, legendas e descrições registram duração da requisição em ms para diagnóstico local.", "check"),
            ReleaseFeature("gemini-usage-charts-157", "Gráficos de uso por função",
                "Painel apresenta tentativas, sucessos, falhas e tendência diária a partir de eventos locais.", "captions"),
            ReleaseFeature("gemini-pricing-estimates-157", "Custos opcionais configuráveis",
                "Tarifas USD/1M informadas por projeto e modelo; estimativas parciais sem afirmar saldo ou cobrança oficial.", "check")
        )),
        ReleaseSummary("0.1.156", listOf(
            ReleaseFeature("gemini-checkpoint-subtitles-156", "Legendas Gemini recuperáveis",
                "Resultados concluídos ficam em cache local temporário, para revisar e restaurar sem nova chamada.", "captions"),
            ReleaseFeature("gemini-caption-draft-156", "Descrição recuperável",
                "Rascunhos de descrição gerados são salvos localmente, com restauração manual e sem duplicar cota.", "check"),
            ReleaseFeature("gemini-failure-class-156", "Erros Gemini mais claros",
                "Falhas de rede, cota, autorização e modelo recebem orientações específicas; tentativas incertas exigem revisão.", "check")
        )),
        ReleaseSummary("0.1.155", listOf(
            ReleaseFeature("gemini-queue-journal-155", "Fila de vozes recuperável",
                "Registra pendentes e WAVs concluídos para retomar lotes somente após confirmação do usuário.", "captions"),
            ReleaseFeature("gemini-queue-uncertain-155", "Revisão de chamadas interrompidas",
                "Chamadas de resultado incerto não são repetidas automaticamente; oferecemos revisão explícita.", "check"),
            ReleaseFeature("gemini-queue-long-155", "Lotes maiores com segurança",
                "Rascunhos e fila local aceitam até 500 falas por projeto com limites de tamanho e escrita atômica.", "check")
        )),
        ReleaseSummary("0.1.154", listOf(
            ReleaseFeature("gemini-usage-windows-154", "Cotas por minuto e por dia",
                "Separação entre capacidade imediata RPM/TPM e projeção diária RPD, usando somente limites locais conhecidos.", "check"),
            ReleaseFeature("gemini-voice-lot-plan-154", "Planejamento de lotes de voz",
                "Identifica WAVs reutilizáveis e gera apenas falas pendentes, sem reiniciar as já concluídas.", "captions"),
            ReleaseFeature("gemini-rpm-pacing-154", "Ritmo local de geração Gemini",
                "Quando configurado, o RPM limita chamadas sequenciais; a geração fica em espera somente com a tela ativa.", "check")
        )),
        ReleaseSummary("0.1.153", listOf(
            ReleaseFeature("gemini-voice-favorites-153", "Vozes Gemini favoritas",
                "Marque vozes com estrela e filtre favoritas, mantendo preferências locais entre projetos e sessões.", "captions"),
            ReleaseFeature("gemini-voice-selected-153", "Nome da voz escolhida",
                "Barra Vozes IA exibe nome amigável da voz geral selecionada e o restaura do catálogo local.", "check"),
            ReleaseFeature("gemini-voice-batch-progress-153", "Progresso por fala concluída",
                "Porcentagem real dos WAVs finalizados em lote; cada solicitação individual indica atividade sem inventar avanço.", "check")
        )),
        ReleaseSummary("0.1.152", listOf(
            ReleaseFeature("ci128-release-notes-limit-152", "CI128: publicação de APKs corrigida",
                "Notas resumidas são geradas para o GitHub Releases; histórico completo permanece no repositório.", "check"),
            ReleaseFeature("ci128-release-notes-guard-152", "Teste preventivo de limite do GitHub",
                "Validação impede publicação com notas acima do limite da API e mantém cinco APKs por arquitetura.", "check")
        )),
        ReleaseSummary("0.1.151", listOf(
            ReleaseFeature("gemini-forecast-151", "Previsões Gemini baseadas em medições",
                "Médias por modelo e operação após cinco amostras e limites manuais por projeto; saldos nunca são inventados.", "captions"),
            ReleaseFeature("gemini-usage-hints-151", "Consumo previsto antes de gerar",
                "Telas de voz, legendas e descrição mostram médias locais quando há dados suficientes.", "check"),
            ReleaseFeature("gemini-safe-fallback-151", "Alternância 429 específica por modelo",
                "Pausa por modelo e projeto preserva outras funções; troca permanece autorizada e restrita a projetos distintos.", "check")
        )),
        ReleaseSummary("0.1.150", listOf(
            ReleaseFeature("ci126-export-480-test-150", "CI126: teste de exportação atualizado",
                "Teste JUnit não rejeita mais 480p, que é opção válida desde 0.1.147; mantém rejeição de resoluções inválidas.", "check"),
            ReleaseFeature("ci126-resolution-cross-check-150", "Resoluções consistentes entre interface e testes",
                "Nova regressão compara 360p, 480p, 720p e 1080p com os valores aceitos pelo coordenador e pelo JUnit.", "film")
        )),
        ReleaseSummary("0.1.149", listOf(
            ReleaseFeature("ci125-material-lock-icon-149", "CI125: painel Gemini compilável",
                "Substituído ícone de cadeado não disponível por ícone Material compatível, mantendo status protegido e ações.", "check"),
            ReleaseFeature("ci125-icon-regression-149", "Regressão de ícone na tela Gemini",
                "Teste evita o símbolo ausente no catálogo Compose e preserva o redesenho das chaves.", "check")
        )),
        ReleaseSummary("0.1.148", listOf(
            ReleaseFeature("gemini-dashboard-redesign-148", "Gemini com painel de credencial redesenhado",
                "Cabeçalho de status e campo protegido em cartões; seis ações compactas sem mudar confirmações sensíveis.", "check"),
            ReleaseFeature("gemini-keys-modal-148", "Múltiplas chaves em cartões e janela de cadastro",
                "Cada chave tem status e ações curtas; adicionar/editar abre janela própria, mantendo proteção e limite de projetos.", "check"),
            ReleaseFeature("gemini-usage-cards-148", "Consumo de IA com indicadores e histórico legível",
                "Aceitas, tokens e avisos 429 com maior destaque; operações técnicas consultáveis por expansão.", "captions")
        )),
        ReleaseSummary("0.1.147", listOf(
            ReleaseFeature("export-low-res-147", "Exportação em 360p e 480p",
                "Opções econômicas com perfis de bitrate próprios, cartões maiores e dimensões verticais H.264 pares.", "check"),
            ReleaseFeature("export-progress-147", "Progresso de exportação mais claro",
                "Percentual informado pelo Media3 com aviso de velocidade variável, sem inventar tempo restante ou avanço.", "captions"),
            ReleaseFeature("saved-video-caption-popup-147", "Descrição após exportar em janela própria",
                "Tela do vídeo salvo com três ações; editor manual e geração Gemini de descrição e hashtags ficam no pop-up com confirmação de cota.", "check")
        )),
        ReleaseSummary("0.1.146", listOf(
            ReleaseFeature("gemini-key-tools-compact-146", "Gerenciador Gemini mais compacto",
                "Seis ações de chave em uma barra de toque acessível, campo protegido e botões de teste/salvar com cantos discretos.", "check"),
            ReleaseFeature("gemini-key-usage-panel-146", "Histórico Gemini mais organizado",
                "Chaves e privacidade em painéis expansíveis; consumo de IA com métricas de respostas, tokens e 429 e detalhes locais sob demanda.", "captions")
        )),
        ReleaseSummary("0.1.145", listOf(
            ReleaseFeature("voice-picker-wide-145", "Seletor Gemini mais amplo e compacto",
                "Diálogo próximo à largura do celular, busca fixa, filtros sem cortes laterais e lista aproveitando a altura disponível.", "check"),
            ReleaseFeature("voice-picker-natural-order-145", "Catálogo mais fácil de navegar",
                "Atendente 2 vem antes de Atendente 10, seleção atual destacada, pesquisa e filtros preservados sem nova chamada Gemini.", "captions")
        )),
        ReleaseSummary("0.1.144", listOf(
            ReleaseFeature("voice-ia-sync-144", "Sincronização assistida de voz",
                "Diferença real de duração, aviso de sobreposição e ajuste WAV offline limitado com original preservado; regeneração guiada exige confirmação.", "captions"),
            ReleaseFeature("voice-ia-filters-144", "Vozes Gemini fáceis de encontrar",
                "Pesquisa, filtros de gênero e altura informados pelo catálogo, nomes curtos e botões compactos por fala.", "check")
        )),
        ReleaseSummary("0.1.143", listOf(
            ReleaseFeature("voice-ia-one-row-143", "Vozes IA em uma barra compacta",
                "Buscar, Lite/Flash, Voz, Humor, Gerar e Inserir na mesma linha, com cantos discretos e contadores curtos.", "captions"),
            ReleaseFeature("voice-ia-sorted-dialogs-143", "Listas de voz e estilo em ordem alfabética",
                "Seletores Gemini com rolagem interna, seleção marcada e todas as opções anteriores preservadas.", "check")
        )),
        ReleaseSummary("0.1.142", listOf(
            ReleaseFeature("voice-ia-general-settings-142", "Voz e humor para todas as falas",
                "Buscar vozes, Lite e Voz geral ficam juntos; estilo Humor geral tem aplicação confirmada, sem substituir escolhas individuais silenciosamente.", "captions"),
            ReleaseFeature("voice-ia-compact-cards-142", "Cartões de voz mais compactos",
                "Legendas em leitura, escolha de voz e estilo por fala, botões curtos e alerta de WAV além do tempo da legenda.", "check"),
            ReleaseFeature("voice-ia-preview-times-142", "Prévia e horários consistentes",
                "Mesma prévia adaptativa de Legendas e horários de início e fim exibidos em décimos de segundo.", "film")
        )),
        ReleaseSummary("0.1.141", listOf(
            ReleaseFeature("ci118-voice-uri-jvm", "CI118: conversão de arquivo compatível",
                "URI da voz Gemini agora funciona nos testes JVM e no Android sem chamar a API Android na política.", "check"),
            ReleaseFeature("ci118-voice-regression", "Regressão de Vozes IA reforçada",
                "Teste de inserção da voz preserva a música e verifica nomes de arquivos com espaço e acentos.", "check")
        )),
        ReleaseSummary("0.1.140", listOf(
            ReleaseFeature("ci117-source-zip-workflow", "CI117: pacote de fonte completo",
                "Incluído o workflow GitHub Actions no ZIP para não manter a versão antiga ao importar o projeto.", "check"),
            ReleaseFeature("ci117-version-sync", "Versionamento sincronizado",
                "Versão do workflow, Gradle e metadados corrigida; nova verificação impede ZIP sem arquivo de CI.", "check")
        )),
        ReleaseSummary("0.1.139", listOf(
            ReleaseFeature("voice-ia-editor", "Vozes IA por legenda",
                "Nova tela acessada pelo menu rolável do editor: legendar, escolher voz Gemini, gerar e ouvir antes de inserir.", "captions"),
            ReleaseFeature("voice-ia-timeline", "Linha Vozes separada",
                "Narrações geradas ficam em trilha própria, com substituição, remoção e ajustes de posição preservando Áudio.", "check"),
            ReleaseFeature("voice-ia-batch", "Vozes em lote com retomada",
                "Permite gerar todas as falas após confirmação e recupera WAVs já preparados, sem refazer chamadas automaticamente.", "check")
        )),
        ReleaseSummary("0.1.138", listOf(
            ReleaseFeature("ci115-karaoke-version-contract", "Teste histórico de karaokê corrigido",
                "O teste de regressão continua verificando os efeitos sem exigir a versão antiga do aplicativo.", "check"),
            ReleaseFeature("ci115-build-validation", "Validação inicial restaurada",
                "Corrigida a verificação que bloqueou o workflow 115 antes da compilação Android.", "captions")
        )),
        ReleaseSummary("0.1.137", listOf(
            ReleaseFeature("subtitle-toolbar-undo-137", "Desfazer na barra principal",
                "O comando Desfazer agora fica ao lado de Salvar, Apagar todas e Buscar, liberando espaço para ver mais legendas.", "captions"),
            ReleaseFeature("subtitle-time-compact-137", "Tempo duplicado removido",
                "A tela mostra um único resumo da legenda selecionada acima da timeline e limpa a repetição ao lado do player.", "check")
        )),
        ReleaseSummary("0.1.136", listOf(
            ReleaseFeature("subtitle-karaoke-accents-136", "Três destaques por palavra",
                "Karaokê com pulso de cor, palavra em negrito ou sublinhada, mantendo os horários existentes.", "captions"),
            ReleaseFeature("subtitle-karaoke-preview-export-136", "Efeitos na prévia e MP4",
                "Uma única regra de cor e destaque é usada no Compose e no Media3; conferir o resultado no aparelho.", "check")
        )),
        ReleaseSummary("0.1.135", listOf(
            ReleaseFeature("ci113-subtitle-stable-preset", "Teste de modelos de legenda corrigido",
                "Corrigido o teste que dependia do último modelo da galeria; agora identifica Etiqueta pelo nome.", "check"),
            ReleaseFeature("ci113-subtitle-preserve-fields", "Regressão para todos os modelos",
                "A verificação protege texto, IDs, horários, posição e tamanho mesmo após reorganizar os presets.", "captions")
        )),
        ReleaseSummary("0.1.134", listOf(
            ReleaseFeature("word-sync-manual-134", "Karaokê ajustável por palavra",
                "Defina manualmente as fronteiras entre palavras com passos de 0,1 s ou digitação; a distribuição é opcional e aproximada.", "captions"),
            ReleaseFeature("word-sync-preview-134", "Prévia do karaokê corrigida",
                "Os horários absolutos são convertidos corretamente para a demonstração de três segundos ou tempo real.", "check"),
            ReleaseFeature("word-sync-safe-134", "Preservação dos horários reconhecidos",
                "Sem mudança automática em textos, cores, projetos antigos ou horários recebidos; salvar é explícito.", "check")
        )),
        ReleaseSummary("0.1.133", listOf(
            ReleaseFeature("subtitle-batch-clarity-133", "Editar todas: seleção e cores corretas",
                "Cópia opcional de cores apenas para palavras iguais, resumo da alteração e desfazer com texto específico.", "captions"),
            ReleaseFeature("subtitle-animations-133", "Novos efeitos e modelos de legendas",
                "Nove animações adicionais, dez estilos prontos e catálogo dividido em categorias.", "film"),
            ReleaseFeature("subtitle-demo-133", "Prévia controlável",
                "Pausar, reiniciar e conferir a duração da legenda; quadros vazios não exibem texto inventado.", "check")
        )),
        ReleaseSummary("0.1.132", listOf(
            ReleaseFeature("ci110-gemini-scroll", "Painel de consumo Gemini corrigido",
                "Corrigido o import de rolagem horizontal que impedia a compilação do painel de consumo.", "check"),
            ReleaseFeature("ci110-gemini-count-tokens", "Contagem de tokens compilável",
                "Corrigido o retorno da consulta opcional countTokens, mantendo consentimento e as regras de privacidade.", "check")
        )),
        ReleaseSummary("0.1.131", listOf(
            ReleaseFeature("gemini-real-token-monitor", "Consumo real da IA em Configurações",
                "Histórico local por projeto e modelo; mede tokens quando o Gemini informa e distingue resultados sem medição.", "check"),
            ReleaseFeature("gemini-preflight-estimate", "Previsão antes de usar a IA",
                "Descrição, Flow, narração e legendas exibem avisos simples; texto tem estimativa local de entrada.", "check"),
            ReleaseFeature("gemini-count-tokens-optional", "Conferir tokens com Gemini",
                "Para texto/Flow, consulte opcionalmente os tokens de entrada antes de gerar; não promete saldo ou preço.", "check")
        )),
        ReleaseSummary("0.1.130", listOf(
            ReleaseFeature("gemini-multiple-keys", "Várias chaves Gemini protegidas",
                "Cadastre até oito credenciais no Android Keystore, nomeie seus projetos Google Cloud, escolha a chave ativa e teste a conexão sem gerar texto.", "check"),
            ReleaseFeature("gemini-project-fallback", "Alternância de projeto após erro 429",
                "Com autorização explícita, pausa o projeto limitado e tenta uma única vez em cada projeto distinto disponível; cotas são por projeto, não por chave.", "check"),
            ReleaseFeature("gemini-legacy-compat", "Compatível com a chave já cadastrada",
                "A chave anterior continua protegida e funcional. Narração, legendas, descrições e prompts Flow aproveitam as chaves cadastradas.", "check")
        )),
        ReleaseSummary("0.1.129", listOf(
            ReleaseFeature("home-three-equal-cards", "Três atalhos compactos na Home",
                "Novo projeto, Animar fotos e Prompts Flow aparecem lado a lado em cartões azuis com ícones, mantendo os fluxos originais.", "film"),
            ReleaseFeature("home-recent-state", "Projetos recentes mais organizados",
                "Cabeçalho compacto, estado vazio mais claro e acesso Ver todos quando existem edições salvas.", "check"),
            ReleaseFeature("home-about-shared-preview", "Prévia da Home preservada em Sobre",
                "A prévia reutiliza os três cartões com ações desabilitadas; novos testes conferem os atalhos reais e o bloqueio na prévia.", "check")
        )),
        ReleaseSummary("0.1.128", listOf(
            ReleaseFeature("export-compact-ui-v128", "Exportação compacta e com ícones",
                "Destinos, resolução e compressão em controles menores; resumo do MP4 e botão Salvar fixos.", "film"),
            ReleaseFeature("export-examples-v128", "Exemplos visuais de enquadramento",
                "Compare Mostrar tudo e Preencher, com avisos de corte e guias opcionais para texto e interface.", "check"),
            ReleaseFeature("export-preview-v128", "Prévia local expansível",
                "Confira o enquadramento com vídeo real; guias não entram no MP4 e a fidelidade total precisa ser validada em Android.", "film")
        )),
        ReleaseSummary("0.1.127", listOf(
            ReleaseFeature("flow-prompt-visibility-ci105", "Correção de compilação do Flow",
                "Corrigida a visibilidade do rascunho de prompts do Flow usada nos projetos, sem alterar os dados salvos.", "check")
        )),
        ReleaseSummary("0.1.126", listOf(
            ReleaseFeature("watermark-v126", "Marca d'água mais fácil de posicionar",
                "Arraste o texto na prévia, use posições rápidas, controle opacidade, fonte, cor, contorno e sombra.", "film"),
            ReleaseFeature("watermark-profile-v126", "Modelo pessoal de marca d'água",
                "Salve e reutilize um padrão local, sem alterar outros projetos automaticamente.", "check"),
            ReleaseFeature("flow-prompt-v126", "Prompts para Google Flow",
                "Monte cenas com estilo visual fixo a partir da Home ou editor; Gemini é opcional e exige aprovação.", "film")
        )),
        ReleaseSummary("0.1.125", listOf(
            ReleaseFeature("subtitle-personal-styles", "Modelos pessoais de legendas",
                "Guarde combinações de fonte, tamanho, cor, contorno, sombra e animação para outros projetos, sem copiar texto ou horários.", "captions"),
            ReleaseFeature("subtitle-style-management", "Renomear ou excluir modelos",
                "Biblioteca local de até 20 modelos com confirmação de exclusão e aviso se não for possível salvar.", "check"),
            ReleaseFeature("subtitle-apply-existing", "Aplicar modelo com prévia",
                "Toque para testar e salve a legenda; Editar todas aplica em lote mediante confirmação, preservando posição e sincronização.", "film")
        )),
        ReleaseSummary("0.1.124", listOf(
            ReleaseFeature("social-destinations", "Exportação para Shorts, Reels e TikTok",
                "Opções de saída 9:16 em 720×1280 ou 1080×1920, mantendo Original como padrão e sem envio automático.", "film"),
            ReleaseFeature("social-framing", "Mostrar tudo ou preencher tela",
                "Escolha entre barras para preservar o quadro ou corte central com aviso de conteúdo perdido; guias visuais opcionais.", "check"),
            ReleaseFeature("social-compression", "Perfil de compressão equilibrado",
                "Ao selecionar rede social, sugere Compacta sem impedir mudar a taxa; diagnóstico compara resolução solicitada e obtida.", "film")
        )),
        ReleaseSummary("0.1.123", listOf(
            ReleaseFeature("publication-project-draft", "Descrições salvas junto ao projeto",
                "Descrição e hashtags permanecem no projeto local e nos backups, sem consumir nova cota Gemini.", "check"),
            ReleaseFeature("publication-editor", "Descrição acessível pelo editor",
                "Nova ferramenta para revisar, editar e copiar o texto de publicação sem exportar o vídeo novamente.", "check"),
            ReleaseFeature("publication-recovery", "Rascunho recuperável",
                "Salvamento automático após a edição e na saída, preservando projetos antigos e cinco hashtags com limite de 100 caracteres.", "check")
        )),
        ReleaseSummary("0.1.122", listOf(
            ReleaseFeature("caption-gemini-models", "Modelos Gemini consultados na sua chave",
                "Escolha entre modelos que suportam generateContent, com consulta manual e preferência local.", "check"),
            ReleaseFeature("caption-json-schema", "Descrição JSON mais consistente",
                "Formato estruturado e validação final de cinco hashtags únicas em até 100 caracteres sem emojis.", "check"),
            ReleaseFeature("caption-errors-privacy", "Falhas explicadas sem repetir chamadas",
                "Mensagens específicas para chave, modelo e limites; a confirmação ainda envia só o texto autorizado.", "check")
        )),
        ReleaseSummary("0.1.121", listOf(
            ReleaseFeature("ci100-androidtest-import", "Compilação dos testes Android corrigida",
                "Corrigido o import inválido de assertExists no teste Home/Sobre que interrompia o workflow 100.", "check"),
            ReleaseFeature("ci-androidtest-import-check", "Regressão protegida no CI",
                "Nova verificação detecta imports inválidos no teste Compose antes de executar o Gradle.", "check")
        )),
        ReleaseSummary("0.1.120", listOf(
            ReleaseFeature("media3-stable-1111", "Media3 estável atualizado",
                "ExoPlayer, efeitos e Transformer alinhados na versão 1.11.1; testar no Android.", "film"),
            ReleaseFeature("export-duration-metrics", "Medições de desempenho da exportação",
                "Detalhes do vídeo exibem o tempo real de processamento e cópia para a galeria, sem supor aceleração.", "check"),
            ReleaseFeature("gallery-buffer-copy", "Cópia de vídeo otimizada",
                "Cópia local em blocos de 256 KiB, preservando verificação de tamanho e limpeza em caso de erro.", "film")
        )),
        ReleaseSummary("0.1.119", listOf(
            ReleaseFeature("pip-caption-layer-parity", "Legendas acima de PiP no MP4",
                "Composição final aplica legendas e marca d'água sobre as janelas PiP, como na prévia do editor.", "film"),
            ReleaseFeature("pip-overlap-order", "Ordem consistente das janelas PiP",
                "Na sobreposição de PiPs, a última janela do editor permanece acima na exportação.", "check"),
            ReleaseFeature("transition-motion-parity", "Zoom e pan na prévia",
                "A prévia do editor compartilha a curva de movimento das transições com o exportador.", "film")
        )),
        ReleaseSummary("0.1.118", listOf(
            ReleaseFeature("export-smoke-android-test", "Teste real de exportação preparado",
                "Cenário Android de recorte, áudio e legenda com verificação de MP4. Sua execução em aparelho permanece pendente.", "film"),
            ReleaseFeature("export-regressions-ci", "Mais verificações automáticas no CI",
                "Compilação dos testes Android e execução de todos os contratos Python antes de publicar os APKs.", "check")
        )),
        ReleaseSummary("0.1.117", listOf(
            ReleaseFeature("saved-video-compact", "Vídeo salvo com visual mais claro",
                "Confirmação compacta com miniatura local, resumo do MP4, compartilhamento e detalhes técnicos recolhidos.", "film"),
            ReleaseFeature("saved-caption-gemini", "Descrição opcional com cinco hashtags",
                "Gerar com Gemini após consentimento; descrição e cinco hashtags ficam em até 100 caracteres, sem emojis.", "check"),
            ReleaseFeature("saved-caption-copy", "Editar, copiar e compartilhar texto",
                "Campos editáveis, contador, botão Copiar e compartilhamento de MP4 com legenda quando compatível.", "check")
        )),
        ReleaseSummary("0.1.116", listOf(
            ReleaseFeature("gemini-compact-editor", "Trecho por vez na narração",
                "Revise e edite a fala selecionada sem percorrer dezenas de caixas de texto; avance ou volte pelos trechos.", "film"),
            ReleaseFeature("gemini-clear-blocker", "Geração sempre acessível",
                "O botão Gerar falas fica fixo e explica quando faltam voz, texto ou um lote válido de até 12 legendas.", "check"),
            ReleaseFeature("gemini-reference-compact", "Áudio de referência mais simples",
                "Navegue uma fala por vez, ouça, analise e marque horários sem repetir comandos para todas as legendas.", "check")
        )),
        ReleaseSummary("0.1.115", listOf(
            ReleaseFeature("gemini-checkpoint", "Recuperar lote de narração",
                "O progresso da narração por trechos é salvo por projeto com texto, horários, voz e WAVs, mesmo após fechar o aplicativo.", "check"),
            ReleaseFeature("gemini-retry-safe", "Retomar somente falas pendentes",
                "WAVs válidos são conferidos por SHA-256 antes de reutilizar; novas chamadas Gemini sempre exigem confirmação.", "check"),
            ReleaseFeature("gemini-batch-12", "Selecionar próximos lotes de legendas",
                "Reutilize até 12 legendas por lote e navegue para as próximas sem reimportar tudo.", "film")
        )),
        ReleaseSummary("0.1.114", listOf(
            ReleaseFeature("gemini-compare-ab", "Comparar voz original e Gemini",
                "Ouça separadamente o áudio do vídeo e o WAV da fala selecionada antes de inserir na linha do tempo.", "film"),
            ReleaseFeature("gemini-duration-goal", "Meta de duração por fala",
                "Ajuste o tempo desejado de 0,25 em 0,25 segundo; o Gemini recebe uma sugestão de ritmo, não um corte do WAV.", "check"),
            ReleaseFeature("gemini-regenerate-one", "Refazer uma única fala",
                "Confirme uma nova geração só para o trecho escolhido, preservando a versão anterior se falhar.", "check")
        )),
        ReleaseSummary("0.1.113", listOf(
            ReleaseFeature("reference-waveform", "Onda sonora na referência de áudio",
                "Visualize energia do áudio próximo à legenda selecionada, sem enviar o som a servidores.", "film"),
            ReleaseFeature("reference-activity-suggestions", "Sugestão local de início da fala",
                "Análise aproximada de atividade sonora; revise antes de usar, pois música e efeitos podem confundir.", "check"),
            ReleaseFeature("reference-limited-decoding", "Análise sob demanda e limitada",
                "Decodifica apenas região curta da fala escolhida e preserva texto, tempo e arquivos originais.", "check")
        )),
        ReleaseSummary("0.1.112", listOf(
            ReleaseFeature("narration-audio-reference", "Áudio de referência para sincronizar falas",
                "Escute o som do vídeo, uma faixa adicional ou arquivo externo e marque o começo de cada fala sem retranscrever legendas.", "film"),
            ReleaseFeature("narration-audio-manual-alignment", "Ajustar início antes da narração",
                "Os horários ajustados são confirmados separadamente e revisados antes de inserir os WAVs; as legendas originais não mudam.", "check"),
            ReleaseFeature("narration-reference-local", "Áudio de referência permanece no dispositivo",
                "O Gemini continua recebendo somente os textos para TTS; referências de áudio não são enviadas e nenhuma voz é clonada.", "check")
        )),
        ReleaseSummary("0.1.111", listOf(
            ReleaseFeature("gemini-timed-cues", "Narração sincronizada por legendas",
                "Gera WAVs separados nos horários originais; a edição de cada fala não altera as legendas do projeto.", "film"),
            ReleaseFeature("gemini-timed-review", "Revisão de conflitos de duração",
                "Mostra falas longas, sobreposição e excesso do fim do vídeo antes de inserir as faixas, sem corte silencioso.", "check"),
            ReleaseFeature("gemini-partial-retry", "Retomar trechos sem repetir síntese",
                "Trechos previamente gerados e inalterados são preservados em caso de erro; confirmação de cota permanece obrigatória.", "check")
        )),
        ReleaseSummary("0.1.110", listOf(
            ReleaseFeature("audio-save-return-editor", "Salvar e voltar ao vídeo",
                "Botão fixo na tela Áudio salva o projeto antes de voltar ao editor; não fecha se houver falha ou corte não aplicado.", "check"),
            ReleaseFeature("audio-ui-readable", "Mixagem mais legível",
                "Nomes automáticos de mídia viram rótulos compreensíveis, cartões mais compactos e avisos dispensáveis, sem mudar volumes.", "check")
        )),
        ReleaseSummary("0.1.109", listOf(
            ReleaseFeature("gemini-audio-commit-in-mixer", "Gemini gera e adiciona à tela Áudio",
                "Após confirmar a geração, salva o WAV e inclui a faixa no projeto sem sair da tela Áudio; falha preserva o áudio para nova tentativa.", "film"),
            ReleaseFeature("audio-mixer-listen", "Ouvir mixagem no topo",
                "Pré-escuta sincronizada do áudio do vídeo, música e faixas adicionais sem alterar a exportação.", "check"),
            ReleaseFeature("editor-preview-volume", "Volume exclusivo da prévia",
                "Controle ao lado do Play reduz todo o áudio ouvido no editor, inclusive PiP, sem mudar os volumes do projeto ou MP4.", "check")
        )),
        ReleaseSummary("0.1.108", listOf(
            ReleaseFeature("audio-extraction-collapsible", "Extração de áudio recolhida",
                "Extrair áudio de vídeo abre por toque e não ocupa toda a tela Áudio por padrão.", "check"),
            ReleaseFeature("gemini-save-return-editor", "Salvar narração e voltar ao vídeo",
                "Depois de confirmar a inclusão do áudio Gemini, o projeto é salvo antes do retorno ao editor.", "film")
        )),
        ReleaseSummary("0.1.107", listOf(
            ReleaseFeature("gemini-voice-visible-confirm", "Escolha de voz correta no filtro",
                "O filtro Femininas/Masculinas não confirma mais uma voz antiga oculta; marcar exige seleção visível.", "check"),
            ReleaseFeature("gemini-voice-radio-click", "Seleção pelo cartão ou círculo",
                "Tocar no círculo agora seleciona a voz, com indicação visível da escolha.", "check"),
            ReleaseFeature("gemini-voice-sample-in-picker", "Ouvir a voz antes de confirmar",
                "Amostra opcional da voz marcada, com cache local e confirmação de possível consumo de cota.", "film")
        )),
        ReleaseSummary("0.1.106", listOf(
            ReleaseFeature("gemini-voice-compact-picker", "Catálogo de vozes mais compacto",
                "Vozes em cartões com nome legível, resumo em português e descrição original opcional em Detalhes.", "check"),
            ReleaseFeature("gemini-voice-filters", "Busca e filtros de vozes",
                "Filtros Todas/Femininas/Masculinas usam dados reais da API; a busca por nome, ID e perfil permanece.", "check"),
            ReleaseFeature("gemini-voice-single-dialog", "Seleção sem duas janelas sobrepostas",
                "Uma janela por vez; Confirmar voz retorna ao vídeo sem gerar áudio e Cancelar preserva a escolha anterior.", "film")
        )),
        ReleaseSummary("0.1.105", listOf(
            ReleaseFeature("animation-source-preview", "Prévia das fotos com nitidez proporcional",
                "Carrega a foto pelo MediaStore/SAF em proporção original e respeita orientação; evita miniaturas quadradas e deformação.", "film"),
            ReleaseFeature("animation-preview-quality", "Qualidade Rápida, Equilibrada ou Alta",
                "Ajuste apenas a decodificação da prévia, sem alterar resolução, taxa ou qualidade da exportação MP4.", "check"),
            ReleaseFeature("animation-preview-expanded", "Prévia ampliada para conferir enquadramento",
                "Visualizador maior usa o mesmo movimento e composição da prévia e mantém a foto original intacta.", "film")
        )),
        ReleaseSummary("0.1.104", listOf(
            ReleaseFeature("github-update-network-diagnostics", "Diagnóstico das atualizações pelo GitHub",
                "Falhas de DNS, timeout, SSL e HTTP mostram mensagens específicas, sem expor tokens nem URLs privadas.", "check"),
            ReleaseFeature("github-update-browser-fallback", "Abrir Releases pelo navegador",
                "A tela de atualizações permite conferir o repositório no GitHub quando a consulta automática falha.", "film"),
            ReleaseFeature("github-update-diagnostic-tests", "Testes para erros de rede e privacidade",
                "Cenários simulados verificam classificação de falhas e proteção contra exposição de mensagens sensíveis.", "check")
        )),
        ReleaseSummary("0.1.103", listOf(
            ReleaseFeature("voice-select-returns-to-video", "Voltar ao vídeo após escolher voz",
                "Confirmar voz fecha a seleção e a narração, retorna ao editor e retoma a prévia; não gera áudio automaticamente.", "film"),
            ReleaseFeature("voice-draft-preserved", "Voz e texto preservados na edição",
                "O rascunho e o catálogo continuam disponíveis quando o usuário reabre Gerar por IA no mesmo projeto.", "check"),
            ReleaseFeature("voice-picker-readable", "Lista de vozes mais legível",
                "Cada voz ocupa uma linha de seleção completa com indicador de marcação e conteúdo alinhado.", "check")
        )),
        ReleaseSummary("0.1.102", listOf(
            ReleaseFeature("gemini-voice-catalog-diagnostic", "Diagnóstico das vozes Gemini",
                "Consulta manual mostra o resultado do filtro pt-BR, fallback geral e motivo de falhas HTTP/rede sem exibir chave nem transcrição.", "check"),
            ReleaseFeature("gemini-voice-catalog-tests", "Testes do catálogo de vozes",
                "Teste offline de resposta vazia, erros de autorização, fallback, paginação e sanitização de diagnósticos.", "check")
        )),
        ReleaseSummary("0.1.101", listOf(
            ReleaseFeature("ci85-buildconfig-version-test", "Teste de versão vinculado ao Android",
                "Sobre e Novidades agora são validados contra o BuildConfig, evitando números antigos nos testes.", "check"),
            ReleaseFeature("ci85-home-history-test", "Validação da Home sem falsas regressões",
                "A verificação da prévia da Home considera as novidades históricas e não exige que uma mudança antiga apareça na versão atual.", "check")
        )),
        ReleaseSummary("0.1.100", listOf(
            ReleaseFeature("github-release-check", "Verificar versões no GitHub ao abrir",
                "Consulta Releases públicas, inclusive pré-lançamentos, sem baixar nada automaticamente.", "check"),
            ReleaseFeature("github-verified-apk", "Baixar e instalar com confirmação",
                "APK universal validado por SHA-256, tamanho, pacote e versão; instalação depende do Android.", "download"),
            ReleaseFeature("github-repository-setting", "Configuração de repositório",
                "O GitHub Actions informa o repositório no build e Configurações permite consultar ou ajustá-lo.", "check")
        )),
        ReleaseSummary("0.1.99", listOf(
            ReleaseFeature("home-preview-about", "Teste visual da tela inicial no Sobre",
                "Visualize as mesmas ações da Home em modo seguro, sem criar projetos; a versão é compartilhada entre Home e Sobre.", "film"),
            ReleaseFeature("manager-version-sync", "Versão reconhecida pelo GitHub Manager",
                "Metadados github-manager.json e app_identity.json sincronizados com Gradle, Sobre e publicação; falha em CI se divergirem.", "check")
        )),
        ReleaseSummary("0.1.98", listOf(
            ReleaseFeature("source-package-gradle-fix", "Projeto Android completo no ZIP",
                "Corrigidos os arquivos Gradle ausentes que impediam a validação e compilação do projeto no GitHub Actions.", "check"),
            ReleaseFeature("source-archive-verification", "Validação do pacote de código-fonte",
                "A entrega passa a verificar os scripts de configuração e a executar a validação também a partir do ZIP extraído.", "check")
        )),
        ReleaseSummary("0.1.97", listOf(
            ReleaseFeature("subtitle-reuse-for-tts", "Reutilizar legendas na narração",
                "Importe localmente legendas manuais, offline ou Gemini para o texto da voz; escolha trechos e confirme antes de substituir o rascunho.", "captions"),
            ReleaseFeature("tts-voice-confirm", "Escolha de voz sob controle",
                "O catálogo só é consultado quando solicitado e a voz só é aplicada após confirmação, sem seleção automática.", "check"),
            ReleaseFeature("tts-timeline-confirm", "Confirmar inclusão de áudio",
                "Antes de adicionar a faixa, veja o início e a duração disponível e confirme a operação.", "film")
        )),
        ReleaseSummary("0.1.96", listOf(
            ReleaseFeature("tts-style-presets", "Mais estilos para narração com IA",
                "São 16 estilos de interpretação, incluindo humor, suspense, conversa e drama, usando a mesma API Gemini.", "check"),
            ReleaseFeature("tts-style-advanced", "Ritmo, emoção e entonação",
                "Ajustes graduais orientam a interpretação; não representam controle numérico exato do áudio.", "check"),
            ReleaseFeature("tts-style-cache-aware", "Amostras respeitam os ajustes",
                "O cache só reutiliza áudio gerado com o mesmo modelo, voz e instruções de estilo.", "film")
        )),
        ReleaseSummary("0.1.95", listOf(
            ReleaseFeature("voice-local-labels", "Vozes com nomes mais claros",
                "O seletor usa rótulos traduzidos para expressões reconhecidas e mantém o nome original e o ID da API.", "check"),
            ReleaseFeature("voice-catalog-details", "Características das vozes",
                "Idioma, apresentação vocal, altura, sotaque, perfil e contexto são exibidos quando enviados pelo catálogo Gemini.", "check"),
            ReleaseFeature("voice-preview-cache", "Reutilização de amostras no aparelho",
                "A primeira amostra pode consumir cota; repetições da mesma voz, modelo e estilo usam cache local sem nova geração.", "film")
        )),
        ReleaseSummary("0.1.94", listOf(
            ReleaseFeature("subtitle-full-playback", "Play contínuo na tela Legendas",
                "A prévia continua da legenda escolhida até o fim do projeto, passando pelos demais clipes sem pausar em cada legenda.", "film"),
            ReleaseFeature("subtitle-timed-overlay", "Legendas sincronizadas na reprodução",
                "A prévia mostra as legendas de cada instante durante o Play, inclusive nos intervalos sobrepostos e sem texto nas lacunas.", "check"),
            ReleaseFeature("audio-quick-ai", "Gerar por IA ao lado de Escolher áudio",
                "O atalho Extrair foi removido da primeira seção; a extração continua disponível no painel independente.", "check")
        )),
        ReleaseSummary("0.1.93", listOf(
            ReleaseFeature("subtitle-sticky-preview", "Prévia fixa durante os ajustes",
                "Nos diálogos de edição individual e em lote, a prévia animada continua visível durante a rolagem.", "film"),
            ReleaseFeature("subtitle-typography-toolbar", "Formatação básica em uma barra",
                "Negrito, itálico e tamanho com botões compactos, sem percorrer sliders extensos.", "check"),
            ReleaseFeature("subtitle-compact-groups", "Controles recolhíveis e aplicação compacta",
                "Palavras, posição e ajustes avançados em grupos; seleção em lote por chips horizontais.", "check")
        )),
        ReleaseSummary("0.1.92", listOf(
            ReleaseFeature("subtitle-cache-cursor-safe", "Correção da validação Android Lint",
                "Cache de áudio agora ignora colunas de data ausentes ou nulas, evitando índice inválido.", "check")
        )),
        ReleaseSummary("0.1.91", listOf(
            ReleaseFeature("subtitle-horizontal-strips", "Estilos e opções em faixas horizontais",
                "Seleção finita de modelos, fontes, animações e cores em cartões compactos.", "film"),
            ReleaseFeature("subtitle-animated-presets", "Modelos com animação incluída",
                "Doze estilos com efeito próprio e três novos movimentos leves.", "check"),
            ReleaseFeature("subtitle-audio-reuse", "Reutilizar extração de áudio",
                "Cache WAV privado para clipes e cortes equivalentes na transcrição Gemini.", "check"),
            ReleaseFeature("subtitle-generated-size", "Tamanho ajustável antes de aplicar",
                "Novas legendas em 20 sp com controle de tamanho e ajuda compacta.", "check")
        )),
        ReleaseSummary("0.1.90", listOf(
            ReleaseFeature("subtitle-exports", "Exportar legendas SRT e TXT",
                "Exportação local pelo seletor Android com tempos preservados em SRT.", "film"),
            ReleaseFeature("subtitle-batch-clear", "Apagar todas e desfazer",
                "Remoção confirmada de todas as legendas com restauração antes da próxima edição.", "check"),
            ReleaseFeature("subtitle-gemini-asr", "Transcrição Gemini opcional",
                "Reconhecimento pela chave já salva, com envio autorizado, horários por palavra e revisão.", "check"),
            ReleaseFeature("subtitle-compact-buttons", "Botões compactos e presets corrigidos",
                "Novos controles retangulares e correção da preservação de opções de sombra.", "check")
        )),
        ReleaseSummary("0.1.89", listOf(
            ReleaseFeature("subtitle-styles-gallery", "Novos estilos de legendas",
                "Galeria com 12 modelos, contornos, sombras, itálico e cores individuais por palavra.", "film"),
            ReleaseFeature("subtitle-timing", "Destaque sincronizado por palavra",
                "Preserva os horários reconhecidos pelo Vosk nas novas legendas e aplica as cores na prévia e exportação.", "check"),
            ReleaseFeature("animate-back-home", "Voltar de Animar Imagens",
                "Voltar retorna à tela inicial após confirmação de rascunho, inclusive ao entrar pelo editor.", "check")
        )),
        ReleaseSummary("0.1.88", listOf(
            ReleaseFeature("project-autobackups", "Cópias automáticas de projetos",
                "Histórico local por intervalo, restauração confirmada e retenção de versões nas Configurações.", "check"),
            ReleaseFeature("backup-saf-export", "Backup externo com pasta escolhida",
                "Exporte, importe e recupere configurações do projeto; os MP4s permanecem em Filmes/UaiCortes.", "film")
        )),
        ReleaseSummary("0.1.87", listOf(
            ReleaseFeature("animation-motion-presets", "Movimentos de câmera controláveis",
                "Animar Imagens oferece zoom de aproximação, afastamento e panorâmica horizontal/vertical, além do modo automático.", "film"),
            ReleaseFeature("animation-preview-settings", "Prévia e configurações mais claras",
                "Prévia com fundo correspondente ao MP4, seleção direta de foto, controles em painel inferior e exportação sem etapa obrigatória de prévia.", "check")
        )),
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

