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
    const val VERSION_NAME = "0.1.258"
    const val VERSION_CODE = 259

    /** Atualizar a entrada mais recente quando houver uma novidade publicável. */
    val recent: List<ReleaseSummary> = listOf(
        ReleaseSummary("0.1.258", listOf(
            ReleaseFeature("ci215-voice-range-258", "Correção do CI 215: páginas da Central de Vozes",
                "Corrigida a exibição do intervalo de 50 vozes: a tela agora usa o segundo valor do Pair retornado pelo paginador. Os dados e filtros existentes são preservados.", "check"),
            ReleaseFeature("ci215-compile-contract-258", "Teste Kotlin para prevenir nova falha",
                "Adicionado teste para impedir o retorno da referência last em Pair e validar os limites das páginas em Kotlin. Ainda requer compilação Android no GitHub Actions.", "check")
        )),
        ReleaseSummary("0.1.257", listOf(
            ReleaseFeature("gemini-voices-pages-257", "Biblioteca de vozes com páginas de 50",
                "Veja até 50 vozes por página, com navegação anterior e próxima. Pesquisa, favoritos e filtros continuam cobrindo todo o catálogo local de 500.", "check"),
            ReleaseFeature("gemini-voices-filters-257", "Filtros rápidos e ações alinhadas",
                "Favoritas, português, gênero e prévias ficam à mão. Renomear, favoritar, ouvir e detalhes agora aparecem alinhados nos cartões.", "check"),
            ReleaseFeature("gemini-voices-performance-257", "Pesquisa e indicadores mais eficientes",
                "Busca com intervalo curto, índice local normalizado, resultados memorizados e diagnóstico opcional de tempos e quadros. Sem geração automática de áudio.", "check")
        )),
        ReleaseSummary("0.1.256", listOf(
            ReleaseFeature("youtube-auto-account-256", "YouTube sem associar manualmente toda vez",
                "Na primeira atualização de canais antigos, escolha a conta Google. Depois de confirmar o ID do canal e o e-mail verificado, a associação fica salva para futuras consultas.", "check"),
            ReleaseFeature("youtube-onboarding-256", "Contas de novos canais identificadas na conexão",
                "O cadastro salva o e-mail autorizado e o ID do canal após verificação; sem guardar tokens ou alterar destinos de publicação.", "check"),
            ReleaseFeature("ci213-horizontal-scroll-256", "CI 213: correção de compilação em Texto para Voz",
                "Corrigido o import de horizontalScroll em Compose, que bloqueou Debug e Release no workflow 213.", "check")
        )),
        ReleaseSummary("0.1.255", listOf(
            ReleaseFeature("google-voice-rename-255", "Renomear vozes Google com um toque",
                "Ícone de lápis diretamente no catálogo, com nomes personalizados locais, sem mudar o ID da voz ou afetar os projetos.", "check"),
            ReleaseFeature("gemini-tts-ptbr-255", "Fala em português brasileiro",
                "As novas narrações Gemini recebem orientação explícita pt-BR. As prévias e a ferramenta Texto para Voz permitem optar pela detecção automática; o sotaque ainda depende do modelo e da voz.", "check"),
            ReleaseFeature("gemini-tts-preview-255", "Prévias coerentes com idioma",
                "Prévia nova usa identidade de cache diferente da geração anterior, enquanto amostras antigas continuam acessíveis no histórico offline.", "check")
        )),
        ReleaseSummary("0.1.254", listOf(
            ReleaseFeature("yt-channel-account-link-254", "Conta Google associada a cada canal",
                "Associe uma vez a conta Google de cada canal YouTube em Ajustes → Minhas contas e canais. A API confirma o ID antes de salvar o vínculo local, sem guardar tokens.", "check"),
            ReleaseFeature("yt-read-only-refresh-254", "Atualização dos canais sem escolha repetida",
                "A consulta usa somente a permissão de leitura e pede a conta associada diretamente ao Google. O sistema ainda pode solicitar consentimento após revogação ou expiração da autorização.", "check"),
            ReleaseFeature("yt-upload-scope-254", "Publicação com permissões separadas",
                "youtube.upload só é solicitado ao confirmar publicação; a consulta de vídeos e métricas não pede permissão de envio.", "check")
        )),
        ReleaseSummary("0.1.253", listOf(
            ReleaseFeature("ci210-workflow-synced-253", "CI 210: versão do workflow sincronizada",
                "O pacote de código-fonte agora entrega o workflow Android junto com o aplicativo, impedindo que a validação use uma versão antiga após a importação.", "check"),
            ReleaseFeature("ci210-source-zip-253", "ZIP completo e validado antes do envio",
                "O empacotador oficial confere o workflow, o versionamento e o código-fonte extraído. Funcionalidades anteriores preservadas.", "check")
        )),
        ReleaseSummary("0.1.252", listOf(
            ReleaseFeature("ci209-export-info-icon-252", "CI 209: correção de compilação na exportação",
                "Corrigida a referência ao ícone informativo na confirmação de exportação, que impedia a compilação Kotlin do aplicativo.", "check"),
            ReleaseFeature("ci209-regression-252", "Verificação contra regressão do ícone",
                "Incluído teste que detecta o símbolo inválido no código de exportação. Projetos, legendas, vozes e redes sociais preservados.", "check")
        )),
        ReleaseSummary("0.1.251", listOf(
            ReleaseFeature("ci208-whatsnew-contract-251", "CI 208: testes da tela Novidades corrigidos",
                "O contrato de regressão reconhece que a tela Novidades abre após atualização e não possui mais entrada duplicada em Configurações ou botão de histórico na apresentação.", "check"),
            ReleaseFeature("ci208-preserve-social-251", "Redes Sociais e publicações preservadas",
                "Mantém as bibliotecas YouTube e Facebook · Páginas, vozes, exportação e projetos existentes sem alterar os dados ou permissões.", "check")
        )),
        ReleaseSummary("0.1.250", listOf(
            ReleaseFeature("meta-page-posts-250", "Redes Sociais: vídeos das Páginas Facebook",
                "Consulte manualmente posts de Páginas autorizadas que contenham anexos de vídeo; filtros e pesquisa local, sem prometer acesso a todos os Reels.", "film"),
            ReleaseFeature("meta-read-permissions-250", "Autorização Meta separada de publicação",
                "Leitura usa páginas autorizadas e pages_read_engagement sem exigir pages_manage_posts. Tokens ficam apenas na memória, sem envios automáticos.", "check"),
            ReleaseFeature("meta-safe-media-250", "Miniaturas seguras e métricas transparentes",
                "Miniaturas disponibilizadas pela Meta são limitadas em tamanho; visualizações ausentes aparecem como indisponíveis, nunca zero inventado.", "check")
        )),
        ReleaseSummary("0.1.249", listOf(
            ReleaseFeature("social-visual-hierarchy-249", "Redes Sociais com destaque para publicação",
                "Cartão de publicação em destaque, seção Visão geral com filtros compactos e métricas com ícones, mantendo consultas sob demanda.", "film"),
            ReleaseFeature("social-video-cards-249", "Cartões de vídeo mais claros e organizados",
                "Miniatura, identificação da plataforma, canal e visualizações organizados em cartões arredondados com grade de duas colunas.", "check"),
            ReleaseFeature("social-empty-search-249", "Busca e estados sem conteúdo aprimorados",
                "Resultados de busca mais legíveis e mensagens de falta de dados e erro com ações orientativas, sem chamadas extras à API.", "check")
        )),
        ReleaseSummary("0.1.248", listOf(
            ReleaseFeature("social-grid-thumbnails-248", "Redes: vídeos em grade com miniaturas reais",
                "Vídeos publicados aparecem em duas colunas, com miniatura do YouTube, canal e destaque de visualizações. A busca abre janela central e filtra os itens carregados.", "film"),
            ReleaseFeature("social-accounts-publish-248", "Contas nas Configurações e publicação com prévia",
                "O cadastro de canais foi centralizado em Configurações. Publicar vídeo mostra um arquivo por vez com miniatura e opção X de remover a seleção.", "check"),
            ReleaseFeature("dialog-updates-248", "Janelas de confirmação mais claras",
                "Exportar e autorizar envio ao YouTube usam janelas adaptáveis com informações resumidas e botões principais visíveis. Novidades pós-atualização mostra só a versão atual.", "check")
        )),
        ReleaseSummary("0.1.247", listOf(
            ReleaseFeature("google-voice-offline-history-247", "Histórico offline de prévias por voz",
                "Reproduza amostras antigas, mesmo após trocar frase ou modelo de comparação; a Central mostra o total salvo por voz e permite filtrar quem já tem prévias.", "music"),
            ReleaseFeature("google-voice-wav-export-247", "Exportar e remover uma única prévia WAV",
                "Salve qualquer amostra pelo seletor Android ou apague somente uma, preservando vozes, favoritos, outras prévias e arquivos de projetos. Ouvir e exportar não chamam Gemini.", "check"),
            ReleaseFeature("google-voice-request-identity-247", "Geração com configuração consistente",
                "O modelo, a frase e o estilo são fixados antes da solicitação para impedir que mudanças na tela salvem o áudio com identificação de outra prévia.", "check")
        )),
        ReleaseSummary("0.1.246", listOf(
            ReleaseFeature("image-import-provider-fallback-246", "Importação de fotos mais resistente",
                "Verifica o formato real da imagem mesmo se a galeria informar tipo genérico ou falhar na leitura de metadados. Confere limites e interrompe cópias canceladas sem deixar arquivos parciais.", "check"),
            ReleaseFeature("project-media-repair-alert-246", "Reparar mídia direto do aviso",
                "Projetos com arquivos inacessíveis mostram uma ação para localizar a origem sem perder cortes, legendas e outros ajustes. A recuperação de foto limpa cópias não salvas.", "film"),
            ReleaseFeature("media-errors-privacy-246", "Erros de mídia mais claros e privados",
                "Diagnóstico registra contagem de mídias inacessíveis, sem nomes ou caminhos, e a interface evita mensagens técnicas fornecidas por aplicativos externos.", "check")
        )),
        ReleaseSummary("0.1.245", listOf(
            ReleaseFeature("update-pause-resume-245", "Atualizações com pausa, retomada e parada",
                "Controle manual do APK parcial, retomada HTTP Range validada e verificação SHA-256 antes de instalar. O servidor pode exigir reinício do download.", "check"),
            ReleaseFeature("subtitle-export-parity-245", "Legendas mais próximas da prévia",
                "Fonte dimensionada proporcionalmente ao quadro em ambas as renderizações, com limite de largura comum. Confira o MP4 real no aparelho.", "film"),
            ReleaseFeature("export-flow-245", "Exportação com confirmação e telas renovadas",
                "Nome sem extensão no editor, confirmação prévia, indicador de etapas, ações diretas no vídeo salvo e atalho Descrição removido da timeline.", "check"),
            ReleaseFeature("diagnostics-workflow-245", "Diagnóstico de download, exportação e saídas do Android",
                "Eventos estruturados sem conteúdo dos arquivos, classificação de encerramentos anteriores e redução de taps repetitivos.", "check")
        )),
        ReleaseSummary("0.1.244", listOf(
            ReleaseFeature("ci203-karaoke-drawbehind-import-244", "Correção da compilação do karaokê rosa",
                "Corrigido o import do drawBehind do Jetpack Compose, que impedia a compilação Debug e Release no workflow 203.", "check"),
            ReleaseFeature("ci203-preserve-home-243-244", "Atalhos roláveis e renomeação preservados",
                "Mantém a faixa de ferramentas da versão anterior, os projetos salvos e o estilo de legenda rosa na prévia e exportação.", "film")
        )),
        ReleaseSummary("0.1.243", listOf(
            ReleaseFeature("home-tools-scroll-243", "Sete ferramentas na faixa superior",
                "Novo projeto, Animar fotos, Estúdio de Cenas, Vozes Google, Texto para Voz, Extrair áudio e Dividir vídeo agora aparecem em cartões azuis com rolagem horizontal, sem duplicatas abaixo.", "film"),
            ReleaseFeature("home-project-rename-243", "Renomear projetos sem abrir o editor",
                "Um botão de lápis aparece nas edições recentes e na aba Projetos. O novo nome é validado e salvo sem modificar clipes, áudio, legendas ou o ID do projeto.", "check")
        )),
        ReleaseSummary("0.1.242", listOf(
            ReleaseFeature("subtitle-pink-karaoke-242", "Karaokê rosa do vídeo",
                "Legendas brancas em maiúsculas com contorno preto; cada palavra recebe uma tarja rosa arredondada sincronizada, também no MP4 exportado.", "film"),
            ReleaseFeature("subtitle-pink-default-242", "Legendas novas já vêm com o estilo rosa",
                "O modo Rosa do vídeo é o padrão das novas legendas e pode ser escolhido na galeria. Seus projetos antigos e os outros estilos continuam disponíveis.", "check")
        )),
        ReleaseSummary("0.1.241", listOf(
            ReleaseFeature("voice-clone-library-241", "Biblioteca de vozes clonadas por personagem",
                "Cadastre referências WAV privadas, escolha Coqui XTTS-v2 ou Chatterbox V3 e reutilize a identidade vocal em outros projetos. Fixe a referência por personagem.", "music"),
            ReleaseFeature("voice-clone-bridge-241", "Gerar voz clonada e inserir na timeline",
                "Servidor Python HTTPS configurável, token só em memória e inserção manual de WAV por fala. Correção dos testes legados que impediam o workflow 201.", "check")
        )),
        ReleaseSummary("0.1.240", listOf(
            ReleaseFeature("voice-speaker-lock-240", "Voz fixa por personagem e humor em cada fala",
                "A voz do personagem prevalece sobre escolhas antigas por fala, enquanto o humor individual continua independente. O lote respeita ambas as configurações.", "music"),
            ReleaseFeature("voice-external-audio-240", "Importar áudio externo em uma fala",
                "Toque em uma fala para abrir ações e importar áudio de forma privada, sem chamada Gemini. O aviso de pendências tem botão de fechar.", "check")
        )),
        ReleaseSummary("0.1.239", listOf(
            ReleaseFeature("media-private-copies-239", "Cópias protegidas fora da galeria",
                "Vídeos, áudios e imagens novos são protegidos em armazenamento privado por projeto, sem criar duplicatas visíveis na Galeria. As mídias exportadas continuam públicas e as cópias anteriores são preservadas.", "check"),
            ReleaseFeature("ci199-channel-test-239", "Correção do workflow 199",
                "Ajustada a contagem de canais com vídeos carregados, eliminando a falha do teste JUnit no workflow 199. Mantidos os totais oficiais do YouTube.", "check")
        )),
        ReleaseSummary("0.1.238", listOf(
            ReleaseFeature("social-unified-238", "Redes com vídeos e métricas unificados",
                "Canais em faixa horizontal com Todos, visualizações, vídeos e inscritos oficiais por canal e vídeos antigos junto dos envios confirmados, sem duplicar IDs.", "film"),
            ReleaseFeature("social-oauth-238", "Menos seleção repetida de contas",
                "A consulta tenta reutilizar autorização da sessão e verifica o ID real do canal. O seletor Google só é forçado quando a conta não corresponde; histórico pendente permanece separado.", "check")
        )),
        ReleaseSummary("0.1.237", listOf(
            ReleaseFeature("editor-preview-stop-237", "Prévia termina com o projeto",
                "A reprodução do editor para ao concluir o último clipe em vez de reiniciar continuamente. Tocar em Reproduzir após o fim volta ao início do projeto.", "film"),
            ReleaseFeature("editor-preview-background-237", "Pausa ao minimizar o Uai Cortes",
                "A prévia é pausada quando o aplicativo perde o primeiro plano, não continua tocando em outro aplicativo e só volta com ação manual.", "check")
        )),
        ReleaseSummary("0.1.236", listOf(
            ReleaseFeature("ci196-social-contract-236", "Correção do workflow 196",
                "Atualiza o teste de regressão do YouTube para a biblioteca que reúne canais e mostra visualizações. Preserva a grade de contas e não altera o envio de vídeos.", "check")
        )),
        ReleaseSummary("0.1.235", listOf(
            ReleaseFeature("social-grid-235", "Contas em grade para economizar espaço",
                "Na aba Redes, os canais e destinos aparecem em quadrinhos lado a lado quando possível, mantendo as ações de verificar, vincular e remover dentro do próprio cartão.", "check"),
            ReleaseFeature("youtube-library-views-235", "Vídeos antigos com visualizações e soma entre canais",
                "A consulta de vídeos do YouTube agora mostra visualizações por vídeo e reúne, em uma lista única, os vídeos já carregados de todos os canais consultados na sessão.", "film")
        )),
        ReleaseSummary("0.1.234", listOf(
            ReleaseFeature("gemini-voice-blind-234", "Escuta de vozes sem mostrar os nomes",
                "Compare duas prévias já salvas, com a mesma frase, modelo e estilo, sem chamadas automáticas à API. Os nomes aparecem após avaliar ou pedir para revelar.", "music"),
            ReleaseFeature("gemini-voice-conflicts-234", "Avaliações de timbre mais consistentes",
                "Prioriza pares ainda não avaliados e evita ocultar vozes em grupos de semelhantes quando você marcou explicitamente um par como diferente.", "check")
        )),
        ReleaseSummary("0.1.233", listOf(
            ReleaseFeature("gemini-manual-similarity-233", "Timbres comparados sem vozes repetidas",
                "A/B direto nos cartões, avaliação local Parecidas ou Diferentes e filtro opcional para ocultar opções que você marcou semelhantes. Sem identificação biométrica nem chamadas extras de áudio.", "music"),
            ReleaseFeature("gemini-multilingual-discovery-233", "Descobrir vozes de outros idiomas",
                "Consulta manual ao catálogo geral do Gemini, preservando as vozes já armazenadas, favoritas, apelidos e prévias. Idioma da voz não garante pronúncia correta em português; uma prévia só é gerada após confirmação.", "check")
        )),
        ReleaseSummary("0.1.232", listOf(
            ReleaseFeature("polly-api-232", "Amazon Polly em Serviços e APIs",
                "Credenciais AWS protegidas por Android Keystore, importação/exportação com confirmação, consulta manual de vozes pt-BR, teste de síntese MP3 e consumo separado da Gemini.", "music"),
            ReleaseFeature("polly-usage-232", "Caracteres e gasto bruto estimado",
                "Histórico local de caracteres confirmados pela API, taxas públicas de referência, limites locais por dia e mês e exportação CSV sem texto nem credenciais.", "check")
        )),
        ReleaseSummary("0.1.231", listOf(
            ReleaseFeature("gemini-voice-center-filters-231", "Central de Vozes mais compacta",
                "Filtros de sotaque, idioma, personalidade e finalidade somente quando presentes nos metadados da API. Pesquisa por apelido, filtros recolhidos e lista progressiva.", "check"),
            ReleaseFeature("gemini-voice-compare-231", "Comparar timbres e personalizar vozes",
                "Comparação A/B com frase e modelo iguais, apelidos locais, prévias curta/padrão/personalizada e configurações reunidas em janela. O Gemini só é chamado após confirmação.", "music")
        )),
        ReleaseSummary("0.1.230", listOf(
            ReleaseFeature("ci191-tts-backhandler-230", "Correção de compilação do Texto para Voz",
                "Corrige a importação do BackHandler de Compose na ferramenta independente Texto para Voz. Mantém Gemini, vozes, contadores, WAV, biblioteca e editor inalterados.", "check")
        )),
        ReleaseSummary("0.1.229", listOf(
            ReleaseFeature("standalone-text-tts-229", "Texto para Voz independente",
                "Nova ferramenta na Início para digitar texto, escolher voz na Central Gemini, selecionar estilo e modelo, gerar WAV com confirmação, ouvir e exportar. Não depende do editor de vídeos.", "music"),
            ReleaseFeature("tts-local-library-229", "Biblioteca de áudios e contadores",
                "Arquivos ficam guardados no aplicativo até exclusão voluntária. Contagem de caracteres, estimativa não oficial de tokens e consumo real da API quando informado; sem chamadas automáticas.", "check")
        )),
        ReleaseSummary("0.1.228", listOf(
            ReleaseFeature("youtube-old-videos-228", "Vídeos antigos do canal no YouTube",
                "Redes permite consultar manualmente a playlist de uploads do canal autorizado, com páginas anteriores e filtro por título. Vídeos existentes na plataforma não são confundidos com envios feitos pelo app.", "film"),
            ReleaseFeature("social-ui-228", "Redes mais organizada",
                "Filtros de publicações em uma linha e consulta de vídeos do YouTube em cartão próprio. Sem varredura automática, importação de dados pessoais ou armazenamento de tokens.", "check")
        )),
        ReleaseSummary("0.1.227", listOf(
            ReleaseFeature("social-layout-227", "Redes com ações sob demanda",
                "Contas mostram nome e vínculo de forma compacta; toque para verificar ou remover. Publicações recolhem as ações secundárias e mantêm os alertas importantes visíveis.", "check"),
            ReleaseFeature("social-structure-227", "Publicação organizada internamente",
                "Telas de Redes e Publicar, diálogos e cadastro público foram separados em componentes, preservando contas, histórico, tokens apenas temporários e confirmação de upload pelo YouTube.", "film")
        )),
        ReleaseSummary("0.1.226", listOf(
            ReleaseFeature("social-history-226", "Histórico de publicações organizado",
                "Redes reúne Pendentes, Recebidas e Revisar. Envios confirmados não exibem compartilhamento como etapa obrigatória; registros antigos permanecem disponíveis.", "check"),
            ReleaseFeature("youtube-processing-226", "Status real do YouTube sob demanda",
                "Para vídeos confirmados, consulta manual à API exibe processamento e privacidade da última verificação. Não confunde vídeo recebido com público e não repete uploads automaticamente.", "film")
        )),
        ReleaseSummary("0.1.225", listOf(
            ReleaseFeature("publish-flow-225", "Publicar vídeo com menos etapas",
                "Após salvar, Publicar vídeo abre o formulário com canal, título, descrição e privacidade. Um único canal abre diretamente a confirmação; o envio continua dependendo de autorização e ação explícita.", "film"),
            ReleaseFeature("youtube-status-225", "Resultado do YouTube mais claro",
                "Progresso ativo separado de resultado não confirmado, título independente do nome MP4 e confirmação de recebimento com acesso ao YouTube Studio. A proteção contra reenvios automáticos foi preservada.", "check"),
            ReleaseFeature("mp4-orientation-225", "Conferência da orientação do MP4",
                "A tela de exportação lê as dimensões e a rotação do MP4 salvo, quando disponíveis, distinguindo a resolução de exibição daquela informada pelo codificador.", "film")
        )),
        ReleaseSummary("0.1.224", listOf(
            ReleaseFeature("social-multichannel-223", "Contas YouTube e Facebook mais simples",
                "Em Redes, Conectar identifica canal ou Página automaticamente, sem cadastrar nomes antes. É possível adicionar mais contas selecionando outra conta Google; o canal é verificado antes da publicação.", "check"),
            ReleaseFeature("youtube-diagnostics-223", "Diagnóstico YouTube mais claro",
                "Erros HTTP 403 agora distinguem API desativada, cota e permissões quando o servidor informa a causa, sem expor tokens ou respostas privadas.", "check")
        )),
        ReleaseSummary("0.1.222", listOf(
            ReleaseFeature("ci184-voice-sheet-222", "Correção de compilação dos ajustes de voz",
                "Os painéis inferiores de Melhorar voz agora usam a declaração compatível com a API experimental do Material 3. Presets, EQ e processamento de áudio continuam iguais.", "check")
        )),
        ReleaseSummary("0.1.221", listOf(
            ReleaseFeature("voice-ui-221", "Ajustes de voz mais simples",
                "Áudio > Melhorar voz agora usa atalhos compactos para Estilo, Ajustes, Equalizador e Limpeza. Controles e perfis aparecem em painéis próprios, sem cortar opções na tela.", "music")
        )),
        ReleaseSummary("0.1.220", listOf(
            ReleaseFeature("voice-studio-220", "Tratamento de voz mais perceptível",
                "Presets reforçados, Estúdio, compressão ajustável, compensação de volume, EQ de quatro bandas, suavização de sibilância, perfis locais e comparação A/B no mesmo trecho.", "music")
        )),
        ReleaseSummary("0.1.219", listOf(
            ReleaseFeature("voice-tone-219", "Voz natural e encorpada",
                "Áudio > Melhorar voz traz presets, intensidade, EQ avançado e comparação com o original. O efeito offline é aplicado à faixa escolhida na exportação, sem alterar a mídia.", "music")
        )),
        ReleaseSummary("0.1.218", listOf(
            ReleaseFeature("ci180-youtube-test-218", "Teste de publicação YouTube atualizado",
                "Correção da validação do workflow após a reorganização de Integrações; publicação e autorização continuam preservadas.", "check")
        )),
        ReleaseSummary("0.1.217", listOf(
            ReleaseFeature("social-integrations-217", "Central de Integrações mais simples",
                "Conexões organizadas por serviço, status honesto e guias passo a passo para APIs, OAuth e compartilhamento manual.", "check"),
            ReleaseFeature("social-compact-217", "Redes Sociais mais compacta",
                "Configuração Meta e métricas ficam recolhidas; publicar, cadastrar contas e vincular canais estão mais fáceis de encontrar.", "check")
        )),
        ReleaseSummary("0.1.216", listOf(
            ReleaseFeature("split-cancel-save-216", "Salvamento mais seguro ao cancelar",
                "A cópia dos MP4 para a galeria verifica o cancelamento durante a gravação e descarta arquivos parciais antes de publicar.", "film"),
            ReleaseFeature("split-duration-ui-216", "Durações e campos mais consistentes",
                "Segundos e Minutos mantêm suas seleções ao alternar; horas, minutos e segundos se adaptam a telas estreitas e fontes ampliadas.", "film")
        )),
        ReleaseSummary("0.1.215", listOf(
            ReleaseFeature("split-social-presets-215", "Cortes alinhados às redes sociais",
                "Novas durações em segundos e minutos, incluindo 1, 2, 3, 4 e 8 minutos, 1min05s, 2min59s e duração personalizada em horas, minutos e segundos. Prévia com intervalos precisos; limites de 180 partes mantidos.", "film")
        )),
        ReleaseSummary("0.1.214", listOf(
            ReleaseFeature("audio-partial-cleanup-214", "Revisão do salvamento de áudio",
                "Após uma falha ou cancelamento, você pode excluir o M4A incompleto com confirmação; o app aguarda a escrita terminar e avisa quando o Android impede a exclusão.", "film")
        )),
        ReleaseSummary("0.1.213", listOf(
            ReleaseFeature("quick-audio-fullscreen-213", "Extração de áudio em tela inteira",
                "Depois de escolher o destino, acompanhe a extração e o salvamento do M4A em tela exclusiva. Ao concluir, abra o áudio salvo ou inicie outra extração; cancelamento com confirmação.", "film")
        )),
        ReleaseSummary("0.1.212", listOf(
            ReleaseFeature("quick-split-fullscreen-212", "Divisão com acompanhamento em tela inteira",
                "Após iniciar o corte, acompanhe cada parte e o salvamento do MP4/M4A em uma tela exclusiva. Ao finalizar, consulte e abra os arquivos realmente salvos, inclusive os concluídos antes de uma interrupção.", "film")
        )),
		        ReleaseSummary("0.1.211", listOf(
            ReleaseFeature("quick-split-accessibility-211", "Dividir vídeo adaptado a telas menores",
                "Cartões de duração se reorganizam em telas estreitas ou com fonte ampliada, e as seleções são mais acessíveis para leitores de tela. Progresso sem percentuais inventados.", "film")
        )),
ReleaseSummary("0.1.210", listOf(
            ReleaseFeature("quick-split-ui-210", "Dividir vídeo com interface renovada",
                "Seleção de vídeo destacada, duração em cartões, opções de áudio mais claras, prévia dos cortes e botão sempre acessível, sem alterar os arquivos de origem.", "film")
        )),
        ReleaseSummary("0.1.209", listOf(
            ReleaseFeature("editor-standalone-tools-209", "Ferramentas independentes fora do editor",
                "Animar imagens e Prompt Flow não aparecem mais na barra do editor. Ambas continuam acessíveis pela tela inicial, sem alterar projetos ou a linha do tempo.", "film")
        )),
        ReleaseSummary("0.1.208", listOf(
            ReleaseFeature("ci172-image-entry-208", "Validação da inserção de imagens corrigida",
                "O teste de regressão acompanha o novo caminho Linha do tempo > Adicionar > Imagem, sem exigir o botão duplicado que foi removido do editor.", "check")
        )),
        ReleaseSummary("0.1.207", listOf(
            ReleaseFeature("protection-media-207", "Mídias mais protegidas durante a edição",
                "Vídeos, áudios e PiPs importados recebem cópias independentes; projetos antigos podem ser protegidos ao reabrir. Os originais não são apagados.", "shield"),
            ReleaseFeature("editor-tools-207", "Ferramentas do editor reorganizadas",
                "Salvar está no cabeçalho, Animar imagens aparece entre as primeiras ferramentas e a linha do tempo concentra a inclusão de mídias.", "film"),
            ReleaseFeature("about-compact-207", "Sobre e Novidades sem repetição",
                "Sobre mostra um destaque e os dados do desenvolvedor, com Pix e GitHub. Novidades continua abrindo após atualização e guarda o histórico.", "check")
        )),
        ReleaseSummary("0.1.206", listOf(
            ReleaseFeature("editor-shortcuts-206", "Ações do editor organizadas",
                "Salvar no cabeçalho, Reparar mídias no menu e Animar imagens em posição visível. Inserção de mídias permanece na linha do tempo.", "film"),
            ReleaseFeature("animation-back-206", "Retorno à edição após animar fotos",
                "Quando aberto pelo editor, Voltar no módulo de animação retorna ao mesmo projeto.", "check")
        )),
        ReleaseSummary("0.1.205", listOf(
            ReleaseFeature("media-copy-205", "Cópia automática dos arquivos importados",
                "Vídeos e PiPs ficam em Filmes, áudio em Música e imagens PiP em Imagens (Android 10+). Fotos da faixa principal já têm proteção privada.", "shield"),
            ReleaseFeature("media-legacy-205", "Proteção de mídias em projetos antigos",
                "Ao abrir um projeto, fontes acessíveis podem ser copiadas com verificação integral e sem substituir uma edição feita durante o processamento.", "folder")
        )),
        ReleaseSummary("0.1.204", listOf(
            ReleaseFeature("subtitle-soft-motion-204", "Legendas com movimentos suaves",
                "Cinco animações novas: Pop suave, Cinema suave, Fade delicado, Impacto limpo e Deslizar suave, com entrada e saída controladas.", "captions"),
            ReleaseFeature("subtitle-shared-motion-204", "Prévia e exportação consistentes",
                "Curvas temporais calculadas no módulo Subtitle Studio, usadas tanto na prévia quanto na exportação, sem novas bibliotecas ou internet.", "film"),
            ReleaseFeature("subtitle-legacy-204", "Estilos anteriores preservados",
                "Os oito modelos principais usam movimentos mais refinados; os 25 estilos antigos e seus efeitos continuam acessíveis.", "check")
        )),
        ReleaseSummary("0.1.203", listOf(
            ReleaseFeature("subtitle-font-ci169-203", "Correção de testes das fontes de legenda",
                "O teste de tipografia agora inclui a fonte Cursiva adicionada ao Subtitle Studio e verifica o fallback para fontes desconhecidas, sem alterar o resultado visual.", "check")
        )),
        ReleaseSummary("0.1.202", listOf(
            ReleaseFeature("subtitle-studio-202", "Subtitle Studio com estilos selecionados",
                "O editor destaca oito aparências modernas e mantém os antigos modelos em uma seção opcional.", "captions"),
            ReleaseFeature("subtitle-case-202", "Maiúsculas sem alterar falas",
                "Mostre legendas em MAIÚSCULAS, minúsculas ou como foram escritas. O texto original, o SRT e as vozes não são alterados.", "check"),
            ReleaseFeature("subtitle-core-202", "Núcleo de legendas modular",
                "Formatação visual de texto em módulo Kotlin independente, reutilizado pelo editor e exportação Media3.", "film")
        )),
        ReleaseSummary("0.1.201", listOf(
            ReleaseFeature("updates-speed-201", "Download com velocidade visível",
                "A tela Atualizações informa a taxa de transferência e os megabytes baixados, com progresso mais claro e botões no padrão visual do aplicativo.", "sync"),
            ReleaseFeature("whatsnew-current-201", "Novidades sem histórico acumulado",
                "Na primeira abertura, só aparecem as mudanças da versão instalada. As versões anteriores permanecem disponíveis por uma ação opcional.", "check"),
            ReleaseFeature("about-history-201", "Sobre e histórico mais organizados",
                "A seção Sobre destaca as últimas atualizações e permite consultar o histórico completo sem ocupar a tela de entrada.", "film")
        )),
        ReleaseSummary("0.1.200", listOf(
            ReleaseFeature("visual-flow-200", "Estúdio Flow e assistentes mais consistentes",
                "Botões principais do Flow, geração em lote e assistentes agora seguem cantos discretos e os formatos compartilhados do Uai Cortes.", "check"),
            ReleaseFeature("visual-voices-200", "Vozes e Gemini padronizados",
                "Seletores, cartões e formulários da Central de Vozes, Vozes IA e chaves Gemini adotam a escala de formatos do aplicativo.", "check"),
            ReleaseFeature("visual-subtitles-trim-200", "Legendas e corte com painéis compactos",
                "Janelas de personagens e sincronização, controles de cortes e painéis usam raios uniformes; miniaturas e indicadores mantêm seus próprios formatos.", "captions"),
            ReleaseFeature("visual-export-home-200", "Exportação e Home com visual alinhado",
                "Ações e diálogos de exportação e vídeo salvo, ferramentas rápidas e painéis inferiores seguem os novos padrões visuais sem mudar os fluxos de edição.", "check")
        )),
        ReleaseSummary("0.1.199", listOf(
            ReleaseFeature("visual-shapes-199", "Interface com cantos padronizados",
                "Botões, cartões e diálogos usam formas discretas compartilhadas nos temas do aplicativo; a janela Salvar edição ficou mais compacta.", "check"),
            ReleaseFeature("pip-compact-199", "PiP compacto com opção de ampliar",
                "A prévia inicia reduzida, há botão Ampliar e controles menores de posição, tamanho, duração e mídia; a edição e exportação permanecem inalteradas.", "check"),
            ReleaseFeature("audio-preview-199", "Ouvir faixa importada isoladamente",
                "A tela Áudio ganhou play/pause direto para faixas adicionais e música. As pré-escutas param ao alternar e não alteram volume nem arquivos do projeto.", "check"),
            ReleaseFeature("audio-sections-199", "Tela Áudio com menos informações redundantes",
                "Mixagem e adição de áudios ficaram mais compactas, volumes e extração começam recolhidos. O extrator avançado do projeto permanece acessível.", "check")
        )),
        ReleaseSummary("0.1.198", listOf(
            ReleaseFeature("quick-gallery-198", "Galeria integrada nas ferramentas rápidas",
                "Dividir vídeo e Extrair áudio agora abrem primeiro a galeria interna, com miniaturas, pesquisa e filtros por pasta, como Criar projeto e Inserir imagem.", "check"),
            ReleaseFeature("quick-gallery-saf-198", "Arquivos do Android continuam opcionais",
                "O seletor de arquivos permanece acessível pela galeria para vídeos fora do catálogo ou sem permissão. Projetos e mídias originais não são modificados.", "check")
        )),
        ReleaseSummary("0.1.197", listOf(
            ReleaseFeature("ci163-lint-media3-audio-197", "Correção de Lint do extrator de áudio",
                "O extrator agora declara opt-in ao Media3 instável internamente, sem exigir a anotação nas telas que o utilizam. Corrige o bloqueio de Lint no workflow 163.", "check"),
            ReleaseFeature("ci163-safety-197", "Ferramentas rápidas preservadas",
                "Dividir vídeo em 9,5 ou 10 segundos e extrair M4A permanecem intactos; incluída regressão para impedir o retorno do erro.", "check")
        )),
        ReleaseSummary("0.1.196", listOf(
            ReleaseFeature("quick-split-196", "Dividir vídeo pela tela inicial",
                "Selecione um MP4 e gere partes independentes de 9,5 ou 10 segundos. A última parte mantém a duração restante e os arquivos originais permanecem intactos.", "check"),
            ReleaseFeature("quick-extract-196", "Extrair áudio sem abrir projeto",
                "Acesse Extrair áudio na Home para criar um M4A local. Na divisão, escolha manter, remover ou extrair também o M4A de cada parte.", "check"),
            ReleaseFeature("quick-split-safety-196", "Processamento sequencial e cancelável",
                "Uma parte por vez, com progresso, cancelamento e salvamento em Filmes/UaiCortes/Divididos. Partes finalizadas não são apagadas ao interromper.", "check")
        )),
        ReleaseSummary("0.1.195", listOf(
            ReleaseFeature("ci161-compose-height-195", "Correção de compilação do editor",
                "Corrigida a referência height ausente no modal de saída, que bloqueou Debug e Release no workflow 161. O layout com três ações horizontais foi preservado.", "check"),
            ReleaseFeature("ci161-regression-195", "Validação de importação Compose",
                "Adicionado teste para verificar a importação de height e impedir a regressão na próxima compilação; nenhuma configuração de projeto ou voz foi alterada.", "check")
        )),
        ReleaseSummary("0.1.194", listOf(
            ReleaseFeature("export-complete-194", "Concluir a exportação salva o projeto",
                "Depois de exportar, toque em Concluir para gravar o projeto no catálogo e voltar à página inicial. Se falhar, o editor permanece aberto; o MP4 já exportado é preservado.", "check"),
            ReleaseFeature("editor-exit-horizontal-194", "Saída mais simples",
                "Salvar, Descartar e Continuar estão lado a lado com ícones; salvar cópia permanece disponível para projetos existentes.", "check"),
            ReleaseFeature("video-placement-194", "Escolha onde inserir vídeos",
                "A ferramenta Vídeo + permite inserir um ou vários arquivos no início, antes ou depois do clipe selecionado ou no final, deslocando os elementos posteriores.", "check"),
            ReleaseFeature("image-placement-194", "Inserção de imagem compacta",
                "As quatro posições da imagem ficam em uma linha e a barra inferior do editor ocupa menos espaço.", "check")
        )),
        ReleaseSummary("0.1.193", listOf(
            ReleaseFeature("voice-simple-list-193", "Vozes IA em linhas compactas",
                "Cada fala tem apenas ouvir/gerar, tom e voz. Toque e segure para refazer, inserir, sincronizar ou remover faixa, sem perder o áudio pronto.", "captions"),
            ReleaseFeature("voice-progress-193", "Progresso fácil de acompanhar",
                "Contador de falas prontas, barra de progresso e ações Gerar/Inserir; personagens, fila salva e informações técnicas sob demanda.", "check"),
            ReleaseFeature("voice-pending-return-193", "Aviso ao retornar ao projeto",
                "Ao reabrir uma geração já iniciada com vozes pendentes, o app oferece continuar, sem disparar API automaticamente ou repetir chamadas incertas.", "check")
        )),
        ReleaseSummary("0.1.192", listOf(
            ReleaseFeature("subtitle-speaker-styles-192", "Aparência por personagem",
                "Defina cores, fonte, animação e posição para todas as falas de um personagem. Edições individuais continuam independentes e podem voltar ao estilo principal.", "captions"),
            ReleaseFeature("subtitle-scroll-192", "Lista de legendas no lugar",
                "Ao editar uma legenda, a lista mantém sua posição. O acompanhamento automático do vídeo permanece disponível quando ativado pelo usuário.", "check"),
            ReleaseFeature("voice-gate-192", "Vozes IA com legendas válidas",
                "Sem legendas, o aplicativo orienta abrir Legendas. A Central de Vozes e a Narração independente não são bloqueadas.", "check"),
            ReleaseFeature("editor-finite-tools-192", "Ferramentas sem rolagem infinita",
                "Os cartões inferiores do editor aparecem uma única vez, com início e fim. O player também acompanha mudanças na duração de imagens do projeto.", "check")
        )),
        ReleaseSummary("0.1.191", listOf(
            ReleaseFeature("ci157-cache-test-191", "Correção do teste da Central de Vozes",
                "Atualizado o teste unitário que bloqueava o APK: a mesma amostra de voz pode ser reutilizada ao trocar apenas a chave Gemini, sem perder a proteção para modelo, voz e interpretação diferentes.", "check"),
            ReleaseFeature("ci157-reuse-191", "Prévia reutilizável preservada",
                "Amostras já armazenadas continuam disponíveis sem nova geração quando a credencial muda; catálogo, personagens, fila e narração mantêm seu comportamento anterior.", "check")
        )),
        ReleaseSummary("0.1.190", listOf(
            ReleaseFeature("voice-center-190", "Central de Vozes do Uai Cortes",
                "Acesse Vozes Google na Home ou nos seletores de Vozes IA e Narração. Catálogo e favoritos compartilhados, com acesso offline à lista salva.", "check"),
            ReleaseFeature("voice-catalog-190", "Catálogo local sem recarga obrigatória",
                "Vozes ficam armazenadas no aparelho, com atualização automática desativada por padrão, botão Atualizar e recuperação da lista anterior quando a rede falha.", "check"),
            ReleaseFeature("voice-samples-190", "Amostras reutilizáveis em todo o aplicativo",
                "Ouça a mesma frase de demonstração, com pontuação e pausas, em vozes diferentes; WAVs ficam salvos por modelo/voz/estilo e não exigem nova API para reproduzir.", "check"),
            ReleaseFeature("voice-actions-190", "Vozes IA mais compacta",
                "A barra destaca Central, tom da fala, gerar pendentes e inserir prontos; personagens continuam herdando sua voz principal com exceções individuais.", "captions")
        )),
        ReleaseSummary("0.1.189", listOf(
            ReleaseFeature("speaker-style-189", "Interpretação por personagem",
                "Defina interpretação Gemini para cada personagem e mantenha ajustes individuais por fala. As escolhas ficam no rascunho do projeto, sem chamadas à API até solicitar geração.", "captions"),
            ReleaseFeature("voice-test-189", "Teste de voz consentido",
                "Ouça uma amostra curta da voz e interpretação escolhidas, após confirmação de uma chamada Gemini que pode consumir cota, sem inserir áudio na timeline.", "check"),
            ReleaseFeature("voice-batch-review-189", "Revisão e geração seletiva",
                "Veja falas pendentes, WAVs preparados, vozes ausentes, áudios desatualizados e ajustes de sincronização, com geração das falas pendentes por personagem ou filtro.", "check"),
            ReleaseFeature("voice-style-queue-189", "Fila de vozes mais segura",
                "A retomada verifica interpretação, modelo e voz antes de reutilizar solicitações; configurações incompatíveis exigem nova confirmação e não disparam chamadas automaticamente.", "check")
        )),
        ReleaseSummary("0.1.188", listOf(
            ReleaseFeature("speaker-voice-188", "Voz Gemini por personagem",
                "Configure uma voz para cada personagem em Vozes IA. Todas as falas identificadas herdam essa escolha, com exceção por legenda e preferência no rascunho do projeto.", "captions"),
            ReleaseFeature("voice-filter-layout-188", "Falas e player mais compactos",
                "Filtro por personagem, busca de legenda, cartões com ícones, prévia recolhida e opção de ampliar mantêm mais espaço útil na tela.", "check"),
            ReleaseFeature("subtitle-style-188", "Legendas IA no estilo SRT ou Karaokê",
                "Escolha a aparência padrão antes de aplicar a transcrição, preservando horários e marcações por palavra quando existirem.", "captions"),
            ReleaseFeature("voice-queue-188", "Fila de voz validada",
                "Ao mudar a voz, uma fila incompatível torna-se obsoleta sem disparar chamadas Gemini ou descartar arquivos WAV anteriores.", "check")
        )),
        ReleaseSummary("0.1.187", listOf(
            ReleaseFeature("androidtest-compile-187", "Compilação dos testes corrigida",
                "Corrigido o import do teste de navegação Home e Sobre que bloqueava a geração dos APKs no GitHub Actions, sem alterar o comportamento do editor.", "check")
        )),
        ReleaseSummary("0.1.186", listOf(
            ReleaseFeature("update-about-186", "Atualizações dentro de Sobre",
                "A tela Sobre mantém Verificar atualizações; não há mais repositório manual, navegador ou atalhos de teste na interface.", "check"),
            ReleaseFeature("update-api-186", "Consulta GitHub mais robusta",
                "Busca Releases mais recentes com limite ampliado de resposta e mensagens específicas; download SHA-256 e instalação consentida continuam integrados.", "check"),
            ReleaseFeature("exit-dialog-186", "Salvar ou descartar com clareza",
                "Diálogo compacto com Salvar projeto, Descartar e sair e Continuar editando; Salvar cópia continua para projetos existentes.", "captions")
        )),
        ReleaseSummary("0.1.185", listOf(
            ReleaseFeature("auto-subtitles-compact-185", "Legendas automáticas mais diretas",
                "Modelo offline, duração e botões de transcrição em layout compacto. Explicações detalhadas ficam no botão de ajuda.", "captions"),
            ReleaseFeature("gemini-consent-readable-185", "Confirmação Gemini sem texto excessivo",
                "Envio só de áudio e aviso de cota/cobrança continuam visíveis; estimativas e cache são consultados em Detalhes.", "check"),
            ReleaseFeature("speaker-chooser-185", "Escolha de personagem simplificada",
                "Diálogo de tamanho ajustável com atribuição manual, cor e seleção. Ajuda separada para contexto da voz.", "check")
        )),
        ReleaseSummary("0.1.184", listOf(
            ReleaseFeature("captions-compact-player-184", "Player e linha do tempo compactos",
                "A barra do vídeo reúne Linha do tempo, navegação, acompanhar e ampliar. Zoom em menu ao lado do progresso; sem mensagens duplicadas.", "captions"),
            ReleaseFeature("captions-speaker-filter-184", "Filtros sem ocultar falas",
                "O filtro de personagem reduz apenas a lista. Todas as legendas permanecem na timeline, com as demais esmaecidas.", "check"),
            ReleaseFeature("captions-tools-184", "Mais espaço para editar",
                "Personagens, busca, ações avançadas e geração automática em menus compactos; excluir tudo continua com confirmação.", "check")
        )),
        ReleaseSummary("0.1.183", listOf(
            ReleaseFeature("gemini-voice-183", "Vozes sugeridas por Gemini",
                "Ao transcrever com Gemini, escolha a detecção de Voz 1, Voz 2 etc. na mesma chamada, sem identificar nomes automaticamente.", "captions"),
            ReleaseFeature("voice-review-183", "Revisar e vincular personagens",
                "Antes de aplicar, crie personagens, vincule a personagens existentes ou deixe falas sem identificação; nenhum vínculo é salvo automaticamente.", "check"),
            ReleaseFeature("voice-safety-183", "Cortes e projetos preservados",
                "Rótulos distintos por transcrição, horários por palavra e recuperação local permanecem; texto e cor não são alterados pela atribuição.", "check")
        )),
        ReleaseSummary("0.1.182", listOf(
            ReleaseFeature("subtitle-speakers-182", "Personagens nas legendas",
                "Cadastre e atribua personagens manualmente, com cor e filtros por falante sem identificação automática.", "captions"),
            ReleaseFeature("subtitle-preview-182", "Prévia compacta e zoom",
                "Amplie a prévia quando quiser e use zoom 1× a 4× na linha do tempo para revisar falas curtas.", "check"),
            ReleaseFeature("subtitle-active-182", "Fala em reprodução destacada",
                "A legenda ativa é indicada separadamente da selecionada, com acompanhamento opcional da lista.", "check")
        )),
        ReleaseSummary("0.1.181", listOf(
            ReleaseFeature("subtitle-video-always-181", "Vídeo visível mesmo sem legendas",
                "A tela de legendas carrega os vídeos e fotos do projeto antes da primeira legenda, com reprodução e cursor de tempo.", "captions"),
            ReleaseFeature("subtitle-track-181", "Linha do tempo com vídeos e legendas",
                "Uma faixa de miniaturas dos clipes permite navegar pelo vídeo; legendas continuam em faixa sincronizada.", "check"),
            ReleaseFeature("subtitle-new-at-cursor-181", "Nova legenda no momento escolhido",
                "Pause no trecho desejado e toque em Nova para criar a legenda exatamente no cursor da reprodução.", "check")
        )),
        ReleaseSummary("0.1.180", listOf(
            ReleaseFeature("vosk-archive-180", "Modelos Vosk importados com segurança",
                "ZIP oficial, pasta externa ou conteúdo extraído são verificados antes da instalação; arquivos inválidos recebem aviso claro.", "check"),
            ReleaseFeature("vosk-crc-180", "Proteção contra arquivos corrompidos",
                "Extração verifica CRC, tamanho, caminhos e entradas repetidas sem substituir um modelo funcional.", "captions"),
            ReleaseFeature("vosk-diagnostics-180", "Diagnóstico de instalação",
                "Erros de rede, modelo e armazenamento têm códigos seguros, sem registrar caminhos ou nomes privados.", "check")
        )),
        ReleaseSummary("0.1.179", listOf(
            ReleaseFeature("editor-confirm-save-179", "Salvar ou descartar ao sair",
                "Ao voltar do editor, escolha salvar o projeto, salvar cópia ou descartar as alterações sem apagar a mídia original.", "check"),
            ReleaseFeature("editor-draft-recovery-179", "Edição temporária com recuperação",
                "Projetos novos só entram no catálogo após confirmação; uma sessão interrompida pode ser recuperada localmente.", "captions"),
            ReleaseFeature("backup-storage-179", "Backups visíveis e limpeza segura",
                "Salvamento e segurança mostra o espaço usado e permite remover backups órfãos com confirmação.", "check")
        )),
        ReleaseSummary("0.1.178", listOf(
            ReleaseFeature("ci147-pip-test-177", "Teste de escala PiP corrigido",
                "O teste agora aceita valores válidos de 91% até 150%, sem liberar valores fora do intervalo de 5% a 150%.", "check"),
            ReleaseFeature("ci147-preserve-editor-177", "Editor e projetos preservados",
                "Correção restrita à regressão do teste JUnit; o posicionamento e enquadramento PiP continuam como antes.", "captions")
        )),
        ReleaseSummary("0.1.176", listOf(
            ReleaseFeature("image-frame-176", "Enquadrar imagens no editor",
                "Selecione uma foto da faixa principal, abra Enquadrar e escolha Ajustar ou Preencher; arraste e amplie com pinça.", "check"),
            ReleaseFeature("image-preview-176", "Zoom salvo por imagem",
                "Zoom e deslocamento ficam salvos por foto, sem alterar o arquivo ou o enquadramento dos projetos antigos.", "check"),
            ReleaseFeature("image-export-176", "Exportação com enquadramento",
                "A prévia e o Media3 usam parâmetros de transformação correspondentes; confirme o resultado no APK e no MP4.", "captions")
        )),
        ReleaseSummary("0.1.175", listOf(
            ReleaseFeature("time-edit-175", "Horários sem perder o valor",
                "Tocar no horário seleciona o valor existente em vez de zerar o campo; alteração parcial e confirmação continuam disponíveis.", "check"),
            ReleaseFeature("pip-frame-175", "PiP sem margens obrigatórias",
                "Tamanho até 150%, alinhamento de borda e opções Ajustar ou Preencher; padrão dos projetos existentes preservado.", "check"),
            ReleaseFeature("compact-timeline-175", "Linha do tempo mais limpa",
                "Faixas vazias são ocultadas; o botão Adicionar oferece acesso a vídeos, imagens, PiP, áudio, vozes e legendas.", "check"),
            ReleaseFeature("transition-cards-175", "Transições visuais compactas",
                "Seis efeitos existentes em cartões visuais, mantendo os demais em Ver todos e a prévia com quadros reais.", "check")
        )),
        ReleaseSummary("0.1.174", listOf(
            ReleaseFeature("timeline-direct-174", "Edite diretamente na linha do tempo",
                "Toque para selecionar, arraste para posicionar e use as alças para aparar vídeos, fotos, PiPs, áudio e legendas.", "check"),
            ReleaseFeature("timeline-source-174", "Limites da mídia respeitados",
                "Vídeos e áudios não ultrapassam a origem; PiP mantém o ponto de leitura na prévia e exportação.", "captions"),
            ReleaseFeature("timeline-history-174", "Zoom, desfazer e refazer",
                "Amplie a régua, use encaixe magnético opcional e reverta gestos sem alterar o arquivo original.", "check")
        )),
        ReleaseSummary("0.1.173", listOf(
            ReleaseFeature("image-bounds-fix-173", "Fotos novamente importáveis",
                "Corrigida a leitura de dimensões que rejeitava imagens válidas na galeria integrada.", "check"),
            ReleaseFeature("photo-regression-173", "Validação da foto ao inserir",
                "JPG e PNG passam por validação de tamanho e cópia privada; arquivo inválido continua sendo rejeitado.", "captions")
        )),
        ReleaseSummary("0.1.172", listOf(
            ReleaseFeature("unified-timeline-172", "Uma linha do tempo para tudo",
                "Faixas com ícones e cursor único sincronizado para vídeos, imagens, PiP, áudio, vozes e legendas.", "check"),
            ReleaseFeature("integrated-gallery-172", "Fotos na galeria integrada",
                "Selecione a imagem no mesmo navegador de vídeo e PiP; uma cópia privada preserva a mídia no projeto.", "captions"),
            ReleaseFeature("api-diagnostics-172", "Diagnóstico com erros de API",
                "Registro local de códigos HTTP e categorias sem guardar credenciais ou conteúdo de solicitações.", "check")
        )),
        ReleaseSummary("0.1.171", listOf(
            ReleaseFeature("trim-visual-171", "Cortar vídeo ficou visual",
                "Tela de cortes com prévia ampla, faixa de quadros interativa e comandos Dividir, Aparar, Remover e Pronto.", "check"),
            ReleaseFeature("trim-safe-remove-171", "Remover um pedaço e unir",
                "Seleção com alças, confirmação, união dos trechos e preservação dos arquivos originais.", "captions"),
            ReleaseFeature("trim-simplified-171", "Menos botões, mais controle",
                "Ajuste preciso e reorganização no menu; desfazer/refazer e compatibilidade com projetos anteriores.", "check")
        )),
        ReleaseSummary("0.1.170", listOf(
            ReleaseFeature("photo-media3-lint-170", "Correção de compilação da capa", 
                "Declaração de opt-in Media3 na montagem de clipes de imagem para corrigir a falha do Android Lint do workflow 143.", "check"),
            ReleaseFeature("photo-editor-preserved-170", "Fotos e capas preservadas",
                "Mantida a inclusão e duração de imagens na timeline sem desativar o Lint ou mudar projetos existentes.", "check")
        )),
        ReleaseSummary("0.1.169", listOf(
            ReleaseFeature("timeline-photo-cover-169", "Capa e fotos na faixa principal",
                "Insira imagens JPG, PNG, WebP ou HEIC da galeria antes do vídeo, entre clipes ou no final; escolha a duração sem produzir MP4 intermediário.", "check"),
            ReleaseFeature("photo-timing-169", "Duração e sincronismo da foto",
                "Edite o tempo de 1 a 30 segundos; legendas, PiPs e trilhas extras posteriores acompanham a inserção da capa.", "captions"),
            ReleaseFeature("photo-media3-169", "Prévia e exportação com fotos",
                "Fotos aparecem como clipes na timeline e usam Media3 para compor o vídeo final, com imagem silenciosa e projeto legado preservado.", "check")
        )),
        ReleaseSummary("0.1.168", listOf(
            ReleaseFeature("meta-sdk-login-168", "Login oficial Meta para Páginas",
                "Autorize a conta pelo SDK Meta, escolha uma Página retornada pela API e vincule seu ID público sem salvar token no catálogo.", "check"),
            ReleaseFeature("meta-session-168", "Sessão Meta para publicar",
                "Após conceder pages_manage_posts, o Reel pode usar o token da Página na sessão atual, ainda com confirmação individual e revisão se houver falha incerta.", "captions"),
            ReleaseFeature("tiktok-oauth-readiness-168", "Configuração OAuth e diagnóstico",
                "Central mostra os requisitos Meta/TikTok; TikTok continua exigindo backend HTTPS próprio e aprovação antes de login integrado.", "check")
        )),
        ReleaseSummary("0.1.167", listOf(
            ReleaseFeature("meta-pages-167", "Reels no Facebook Página — modo avançado",
                "Vinculação de Página por token temporário, verificação de identidade e envio confirmado antes de iniciar; perfil pessoal continua assistido.", "check"),
            ReleaseFeature("tiktok-direct-167", "TikTok com prévia das permissões",
                "Consulta da conta e privacidade atual, envio em partes mediante confirmação e acompanhamento de processamento por sessão.", "captions"),
            ReleaseFeature("social-no-duplicate-167", "Publicações sem duplicação automática",
                "Status persistente antes de enviar, token descartado ao fechar e revisão obrigatória após resultado incerto.", "check")
        )),
        ReleaseSummary("0.1.166", listOf(
            ReleaseFeature("youtube-oauth-166", "Autorização YouTube pelo Android",
                "Vincule o ID público do canal após permissão Google; credenciais não são exportadas nem salvas no catálogo.", "check"),
            ReleaseFeature("youtube-upload-166", "Envio direto YouTube com confirmação",
                "Upload de vídeo por API com escolha de privacidade, progresso e revisão obrigatória após falha incerta.", "captions"),
            ReleaseFeature("youtube-metrics-166", "Métricas reais do YouTube",
                "Consulta manual de visualizações e interações por vídeo confirmado, separada dos dados manuais.", "check")
        )),
        ReleaseSummary("0.1.165", listOf(
            ReleaseFeature("social-dashboard-165", "Nova aba Redes Sociais",
                "Destinos separados para Facebook Página/Perfil, YouTube, TikTok e Kwai, com histórico local e métricas inseridas manualmente.", "check"),
            ReleaseFeature("social-publish-165", "Publicação assistida após exportação",
                "Seleção de contas, descrição e hashtags comuns, compartilhamento individual via Android e confirmação manual sem duplicidade automática.", "captions"),
            ReleaseFeature("social-api-center-165", "Central de APIs e Contas",
                "Links oficiais, acesso ao Gemini e importação/exportação somente de configurações públicas. OAuth social permanece pendente.", "check")
        )),
        ReleaseSummary("0.1.164", listOf(
            ReleaseFeature("studio-wizard-164", "Estúdio de Cenas com Wizard visual",
                "Cinco etapas: estilo, personagens, cenários, cenas e exportação, com detalhes opcionais e navegação simples.", "check"),
            ReleaseFeature("studio-library-164", "Personagens e cenários reutilizáveis",
                "Sugestões locais prontas, biblioteca compartilhada e referências da galeria sem geração obrigatória de imagem.", "captions"),
            ReleaseFeature("studio-enrich-164", "Gemini ajuda a criar referências",
                "Enriquecimento individual de personagem/cenário com confirmação, revisão, consumo rastreado e opção offline.", "check")
        )),
        ReleaseSummary("0.1.163", listOf(
            ReleaseFeature("flow-one-clip-163", "Cada cena é um vídeo do Flow",
                "Criação direta de clipes sem roteiro completo; imagem, fala, ação, câmera e sons por cena.", "check"),
            ReleaseFeature("flow-manual-163", "Roteiro sem geração obrigatória",
                "Prepare prompt em inglês localmente ou peça aprimoramento ao Gemini sob confirmação.", "captions"),
            ReleaseFeature("gemini-global-163", "Chave Gemini global e troca 429 no Estúdio",
                "Seleção de credenciais compartilhada entre módulos; geração de texto e imagem com failover opt-in entre projetos.", "check")
        )),
        ReleaseSummary("0.1.162", listOf(
            ReleaseFeature("flow-guided-162", "História revisável sem formulários longos",
                "Ideia única, ajustes recolhidos e roteiro do Gemini em leitura direta com edição opcional.", "check"),
            ReleaseFeature("flow-scene-dialogues-162", "Cenas por personagem, fala e tempo",
                "Cartões mostram personagens, falas, câmera e sons; edição estruturada e duração estimada por cena.", "captions"),
            ReleaseFeature("flow-task-feedback-162", "Progresso e cancelamento identificados",
                "Surpreenda-me avisa que é local; Gemini mostra a operação e permite cancelar chamada incerta.", "check")
        )),
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

    /** Sobre exibe uma única alteração; histórico completo apenas em Novidades. */
    fun forAbout(): List<ReleaseSummary> = listOf(current.copy(features = current.features.take(1)))
    /** A abertura após atualização mostra somente as mudanças da versão instalada. */
    fun forWhatsNew(): List<ReleaseSummary> = listOf(current)
    /** A consulta histórica é sempre explícita; não polui o primeiro acesso. */
    fun forHistory(): List<ReleaseSummary> = recent.drop(1)
    fun forOlderAbout(): List<ReleaseSummary> = recent.drop(3)

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

