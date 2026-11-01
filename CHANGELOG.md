## v0.1.96 (versionCode 97) · 01/11/2026

- Narração Gemini: 16 estilos de interpretação, mantendo os cinco anteriores, com rótulos em português e instruções em `speech_metadata.style`.
- Ajustes avançados opcionais: ritmo, expressividade emocional e altura/entonação em cinco níveis cada, restauráveis aos valores naturais. Não prometem m/s, Hz ou intensidade acústica exata.
- Uma instrução composta é usada tanto na amostra quanto na geração definitiva; cache considera a instrução completa. Presets antigos com ajustes naturais mantêm a mesma chave de cache da v0.1.95.
- Preserva texto narrado literalmente, autorização explícita antes de solicitar serviço, vozes/IDs, histórico de amostras, edição offline e `schemaVersion=4`.
- Compilação completa Android e validação da fidelidade dos estilos no aparelho ainda pendentes.

## v0.1.95 (versionCode 96) · 01/11/2026

- Catálogo Gemini: nomes traduzidos para expressões conhecidas, preservando nome original e ID técnico; características só aparecem quando fornecidas pela API. Busca cobre tradução, nome, ID e metadados.
- Ouvir amostra: lê primeiro do cache interno, com chave derivada criptograficamente da conta, modelo, voz, estilo e frase fixa. A primeira síntese continua exigindo confirmação e pode consumir cota; reutilizações não fazem nova chamada.
- Cache WAV privado: validação de formato, limite de 24 amostras / 12 MiB, expiração após 30 dias sem uso e limpeza automática possível pelo Android. Amostras não são anexadas aos projetos.
- A API de catálogo não fornece `sample_audio` para vozes predefinidas; não é apropriado assumir acesso direto às prévias existentes no site do Google.
- Não modifica estilos de narração nem a reprodução de legendas; `schemaVersion=4` mantido.

## v0.1.94 (versionCode 95) · 01/11/2026

- **Legendas — reprodução contínua:** selecionando uma legenda, a prévia começa naquele instante, mas o Play não pausa mais em `endMs` da legenda. O Media3 continua pelos clipes seguintes até `STATE_ENDED` ou pausa manual. Ao reproduzir novamente após o fim, começa no início do projeto.
- **Legenda no tempo real:** enquanto reproduz, as legendas exibidas acompanham o relógio da timeline, incluindo legendas seguintes, lacunas sem texto e sobreposições; a seleção de edição permanece independente. Ao pausar, a legenda selecionada volta a ser editável; o botão Reiniciar legenda volta ao início da seleção.
- **Áudio — atalhos sem duplicação:** a primeira seção agora contém Escolher áudio e Gerar por IA lado a lado. Extrair áudio de vídeo mantém sua seção separada e suas opções de origem/formato/salvamento.
- **Compatibilidade:** sem mudanças na API Gemini, vozes, cotas, exportação, mídia ou schemaVersion 4. A v0.1.95 fica reservada para melhorias de catálogo/prévias.
- **Validação:** teste novo para intervalos consecutivos, lacunas, sobreposições e fim. Build Android, lint e ensaio de reprodução real devem ser confirmados via CI/aparelho antes de anunciar APK.

## v0.1.93 (versionCode 94) · 01/11/2026

- **Prévia fixa:** o exemplo animado das legendas fica fora da área rolável dos diálogos Editar legenda e Editar todas; permanece visível enquanto navega pelas opções. A prévia principal da tela de legendas já era fixa acima da lista.
- **Formatação compacta:** negrito, itálico e tamanho ficam em uma única barra; botões de −/+ alteram 2 sp por toque, respeitando 12–48 sp. Presets animados horizontais passam de 158 para 142 dp de largura e 64 dp de quadro.
- **Opções sob demanda:** cores por palavra, posição (horizontal/vertical) e efeitos avançados (fonte, alinhamento, contorno, sombra e espaçamento) recolhíveis. Nenhuma função foi removida.
- **Editar todas:** caixas de seleção verticais substituídas por três chips finitos Estilo/Animação/Posição, com a mesma confirmação e preservação dos textos e tempos.
- **Segurança e compatibilidade:** nenhum campo persistido ou motor de legendas foi alterado; mantém schemaVersion 4. Não mexe em áudio, cache nem na API Gemini.
- **Pendente:** Gradle/Android Lint e validação visual em aparelho; testes locais não confirmam pixels nem exportação MP4.

## v0.1.92 (versionCode 93) · 01/11/2026

- **CI 77:** `:app:lintDebug` reprovou por `Range` em `SubtitleAudioCache.kt:57`, pois `Cursor.getColumnIndex()` pode devolver `-1`.
- **Correção:** o índice de data de modificação passa por verificação `>= 0`, com tratamento de coluna `NULL`, antes da leitura com `Cursor.getLong()`. A ausência de metadados continua usando `-1L`, sem invalidar o cache ou mascarar a falha.
- **Regressão:** `tools/validate_source.py` verifica a condição segura e a ausência de acesso `getLong(getColumnIndex(...))`. Sem desativar Android Lint.
- **Versão:** Gradle, CI dos cinco APKs Release, empacotamento, Sobre/Novidades, README e documentação sincronizados em 0.1.92/93. A v0.1.91 compilou Debug/Release, passou pelos testes unitários e gerou `assembleRelease` nos logs; a publicação foi bloqueada por `lintDebug`.
- **Pendente:** executar novamente CI para confirmar o Lint e publicar os APKs. Nenhum teste físico de legendas/exportação foi efetuado nesta correção.

## v0.1.91 (versionCode 92) · 01/11/2026

- Opções visuais de legendas organizadas em rolagem horizontal finita, inclusive cores, fontes, alinhamentos e animações. Galeria de 12 cartões compactos animados.
- Escolha de modelo também define uma animação real; adicionados Bounce, Pulsar e Zoom reverso à prévia e exportação baseadas em SubtitleAnimations.frame.
- Novas transcrições Vosk/Gemini usam tamanho inicial de 20 sp em lugar de 30 sp. Controle 12–30 sp na revisão antes de aplicar; legendas antigas inalteradas.
- Áudio WAV reutilizável em cache privado para chamadas Gemini equivalentes, com hash de clipes/cortes, verificação de comprimento, retenção máxima de 3 horas e limpeza limitada. Nova chamada de API continua necessária.
- Ajuda de posicionamento movida para ícone na linha do tempo; menos texto persistente na tela.
- Build Gradle/Android e testes de exportação e API em dispositivo pendentes.

## v0.1.90 (versionCode 91) · 01/11/2026

- Corrigido o teste de presets: o deslocamento de sombra personalizado é preservado quando um modelo não define sombra; modelos com sombra explícita continuam aplicando seu próprio valor.
- Barra da tela Legendas em grade 2×3 com cantos 8 dp; importar SRT, exportar SRT/TXT (SAF) e apagar todas as legendas após confirmação, com opção Desfazer.
- Gemini ASR opcional pela chave protegida já salva nas Configurações: extrai WAV temporário da ordem de clipes/cortes, pede consentimento antes do envio, usa Interactions API Gemini 3.5 Transcribe com tempos por palavra e permite revisar antes de aplicar. Requer internet e pode consumir cota/cobrança.
- Limite de 10 min; o modo Vosk offline segue independente, e projetos existentes continuam com schemaVersion=4.
- Verificar compilação/limites de API, sincronismo e exportação no Android físico antes de declarar funcionamento confirmado.

## [0.1.89] - 2026-11-01

- Nova galeria visual com 12 estilos de legendas e recursos de personalização de cores por palavra.
- Anotações de tempo por palavra do Vosk preservadas no JSON; karaokê usa tempos observados e mantém fallback para legendas sem tempos individuais.
- Texto itálico e cores de palavra compartilhadas entre prévia e renderização MP4.
- Corrigido o destino do botão Voltar da ferramenta Animar Imagens, com proteção de rascunhos não salvos.
- Próximo versionCode = 90; testes Gradle e dispositivos ainda pendentes.

## [0.1.88] - 2026-11-01

- Salvamento de projetos: histórico de cópias locais automáticas com intervalos de 15 s, 30 s, 1, 2 ou 5 minutos, sem duplicar conteúdo e com retenção de 3/5/10 versões.
- Configurações > Salvamento e segurança: escolher frequência, quantidade de cópias, pasta SAF externa, exportar/importar JSON íntegro SHA-256 e confirmar restauração.
- O cadastro do projeto continua salvo a cada edição; cópias históricas são uma proteção adicional. Ao restaurar, o app tenta salvar o estado atual antes de substituí-lo.
- Indicador de mídias não acessíveis ao abrir um projeto. Backup JSON não copia vídeos/fotos/músicas; o MP4 exportado permanece em Filmes/UaiCortes, não Downloads.
- Não foi acrescentado backup em nuvem nem alteração ao esquema JSON 4. A recuperação após limpar dados/desinstalar requer arquivo exportado ou pasta externa acessível.

## v0.1.87 (versionCode 88) · 01/11/2026

- **Animar Imagens:** cinco padrões de câmera (automático, aproximar, afastar, panorâmica horizontal e vertical) aplicados pela mesma função de quadro à prévia e ao Media3; mantendo a opção `auto` em rascunhos antigos.
- **Interface de Animar Imagens:** configurações organizadas em painel inferior rolável; reprodução guiada por tempo real de quadros; toque para inspecionar foto na sequência; cabeçalho “Rascunho” diferenciado de “Gerar MP4”.
- **Prévia:** fundo da própria foto ampliado e escurecido, foto principal sem corte, e transformação sobre toda a composição; ainda é prévia aproximada, sujeita a diferença em transições e hardware.
- **Rascunhos e exportação:** novo campo opcional `motion` no JSON sem perder a leitura dos rascunhos antigos; mantém renderização H.264 offline, duração e formato, sem alterar projetos de vídeo ou `schemaVersion=4`.
- **Validação:** código e testes locais; renderização e comparação visual no Android físico permanecem pendentes.

## v0.1.86 (versionCode 87) · 01/11/2026

- **Correção do workflow Gerar APK 72:** `:app:lintDebug` reprovou com 2 erros `UnsafeOptInUsageError` no `VideoEffects.kt`, ao acessar `TextOverlay.TEXT_SIZE_PIXELS` nos spans de contorno e sombra.
- **Correção localizada:** `@androidx.annotation.OptIn(UnstableApi::class)` nas duas classes, preservando o dimensionamento e estilo das legendas e sem suprimir inspeções do Android Lint.
- **Prevenção de regressão:** verificação estrutural de ambos os opt-ins e da importação correta, com teste específico; metadados de versão e workflow sincronizados.
- **CI 72 confirmado nos logs:** compilação Debug/Release e `:app:assembleRelease` executadas; bloqueio no Lint impediu a publicação dos cinco APKs.
- **Pendente:** repetir o workflow integral de Gradle (`testDebugUnitTest`, `lintDebug`, `assembleRelease`) e verificar geração/publicação dos APKs.

## v0.1.85 (versionCode 86) · 01/11/2026

- **Correção do workflow Gerar APK 71:** `WhatsNewReleaseNotesTest.reorderingOrChangingACaptionChangesTheFingerprint` falhava porque a versão 0.1.84 contém uma única novidade e a lista invertida é igual à original (288 de 289 testes passaram).
- **Teste corrigido, regra de produto preservada:** reordenação agora é verificada com duas novidades artificiais apenas no teste; um novo teste garante que uma novidade isolada tem a mesma impressão digital quando invertida, mas mudanças no texto alteram a impressão digital. Sem adicionar itens artificiais na interface.
- **Confirmado no CI 71:** `compileDebugKotlin` e `compileReleaseKotlin` concluíram com avisos não impeditivos. Publicação dos cinco APKs interrompida nos testes; não houve falha de assinatura identificada.
- **Compatibilidade:** nenhum comportamento do exportador, Media3, Gemini, legendas, PiP ou `schemaVersion 4` alterado. Assinatura e workflow mantidos.
- **Pendente:** executar o workflow completo para confirmar todos os testes, lint e geração de cinco APKs na versão 0.1.85.

## v0.1.84 (versionCode 85) · 01/11/2026

- **Correção do workflow Gerar APK 70:** compilação Debug e Release falharam em `ExportEngine.kt` (linhas 177 e 179) porque `start()` era pública e expunha `ExportBitratePreset` e `ExportQualityReport`, que são tipos internos.
- **Ajuste mínimo:** `ExportEngine.start` passou a `internal fun start`; mantém tipos, parâmetros, callbacks, bitrates, resolução e comportamento de exportação sem mudanças.
- **Regressão:** verificador estrutural agora exige visibilidade compatível nos três símbolos. A validação de sintaxe e o teste isolado do compilador Kotlin não substituem o build Android completo.
- **Preservados:** cinco APKs Release via GitHub Actions, Secrets de assinatura, schemaVersion 4, PiP, Gemini, legendas e projetos.
- **Pendente:** executar workflow para confirmar `compileDebugKotlin`, `compileReleaseKotlin`, testes/lint e publicação dos cinco APKs.

## v0.1.83 (versionCode 84) · 01/11/2026

- **Revisão crítica de v0.1.81 e v0.1.82:** confirmados o uso de `Presentation.createForShortSide`, ordem de redimensionamento antes das legendas e propagação de bitrate. O Media3 pode ignorar parâmetros solicitados; sem testes Android, a melhora visual ainda não está comprovada.
- **Correção de espaço:** a reserva do MP4 temporário e da cópia MediaStore deixa de truncar projetos acima de 24 horas. Cálculos em Long agora saturam em vez de transbordar; estimativa MiB satura em `Int.MAX_VALUE` para durações extremas.
- **Mensagens:** falta de espaço indica o perfil real (Compacta/Alta/Máxima), sem afirmar que toda exportação usa Alta.
- **Regressões:** adicionados testes de duração >24h, saturação e equivalência de reserva para um minuto; preservados os demais 15 testes locais de exportação.
- **Pendente:** teste Gradle/JUnit completo, exportação em aparelho com legendas/PiP, medição de taxa e resolução no MP4, desempenho e comparação após publicação.

## v0.1.82 (versionCode 83) · 01/11/2026

- **Três perfis selecionáveis de bitrate de vídeo:** Compacta, Alta (padrão anterior) e Máxima. Valores solicitados em 720p: 5/8/12 Mb/s; em 1080p: 10/18/28 Mb/s. O encoder pode ajustar o valor e maior bitrate não restaura detalhes perdidos no arquivo-fonte.
- **Estimativas e reserva de espaço coerentes com a escolha:** o tamanho esperado por minuto e o teste preventivo de armazenamento agora usam o perfil selecionado, inclusive na prevenção de falta de espaço para MP4 temporário e cópia na galeria.
- **Aviso de reescala da origem:** consulta dimensões dos clipes em segundo plano e avisa quando o lado menor do vídeo de origem é inferior à resolução escolhida. Não apresenta resolução inventada quando o arquivo não fornece metadados.
- **Tamanho previsto para o projeto:** exibe estimativa aproximada de MiB para a duração total e o perfil selecionado, além da referência por minuto.
- **Quadros-chave:** solicita ao codificador H.264 intervalo de 2 segundos, com fallback do Media3; não afirmar GOP obtido sem verificar o MP4.
- **Relatório de exportação:** diferencia bitrate solicitado e medido, identifica o perfil e fornece nota contextual quando a taxa medida ficar abaixo de metade do alvo. Taxa baixa pode refletir cenas simples, não é diagnóstico definitivo de degradação visual.
- **Compatibilidade:** preserva 720p/1080p pelo lado menor, texto renderizado depois de dimensionar, PiP, legendas, áudio, Gemini, Vosk, `schemaVersion=4`, cinco APKs Release, assinatura e Secrets. Não altera o motor de animações de imagens.
- **Validação pendente:** build Android Gradle e teste físico de MP4 nos três perfis, com legendas e PiP; conferir bitrate e intervalos de quadros-chave no arquivo final antes de afirmar entrega do encoder.

## v0.1.81 (versionCode 82) · 01/11/2026

- **Nitidez de legenda no MP4:** reordena os efeitos do Media3 para normalizar o lado menor do clipe (720 ou 1080 px) ANTES de renderizar legendas e marca d’água, evitando ampliar uma camada de texto rasterizada na resolução menor da fonte. Corrige especialmente exportações verticais 9:16: o modo 1080p passa a solicitar 1080×1920, em vez de cerca de 608×1080. O ajuste de tamanho e posições continua usando o mesmo schema 4. A prévia Compose e o MP4 ainda exigem teste visual no aparelho.
- **Contorno/sombra:** usa tamanho tipográfico consistente com o RelativeSizeSpan no renderizador Media3, evitando dupla multiplicação de escala no desenho dos efeitos.
- **Diagnóstico real do arquivo exportado:** resolução, bitrate de vídeo, quadros por segundo médios estimados, tamanho, codec e alerta quando a altura obtida for diferente da solicitada. Valores indisponíveis são informados como tais, sem estimativa falsa; a confirmação só aparece após o MediaStore salvar.
- **Segurança e estabilidade:** não altera Gemini, PiP, áudio, projeto, assinatura, cinco saídas de APK nem o `schemaVersion=4`. Mantém fallback do encoder para compatibilidade com diferentes aparelhos.
- **Verificações pendentes:** compilação Gradle Android, teste de PiP e legendas em MP4 de 720/1080 e comparação com a publicação nas redes sociais. A imagem publicada não permite diagnosticar sozinha qual etapa gerou a perda de nitidez.

## v0.1.80 (versionCode 81) · 31/10/2026

- **Correção do workflow Gerar APK 67:** falha unitária `SubtitleExportSizingTest.longWordNeverExceedsFrameAndNoNaN` causada por escala mínima de 48% para uma palavra que precisava de redução maior. A tarefa `:app:testDebugUnitTest` interrompia a Release após 270/271 aprovações.
- **Ajuste responsivo de legendas:** `SubtitleAutoFit.factor` agora aplica piso de 48% apenas à redução causada pelo comprimento total da frase; uma palavra isolada pode exigir redução adicional para caber dentro da margem horizontal do quadro, tanto no Compose como no exportador Media3.
- **Escala na exportação:** removido piso absoluto de 0,01 em `SubtitleExportSizing.relativeFontSize`, que poderia reintroduzir corte em casos extremos. Textos, tempos, posição das legendas e esquema de projetos permanecem inalterados.
- **Regressão:** dois novos testes JUnit cobrem palavra longa, limite visual de frases e medidas extremas; execução isolada por runner compatível JUnit: 11/11 aprovados, inclusive o teste que falhou no CI. Compilação Android/Gradle completa ainda pendente.
- **Compatibilidade:** sem alterações nos recursos de PiP, Gemini TTS, Vosk, mediação de áudio, exportação, assinatura ou cinco ABIs. `schemaVersion=4` mantido.

## v0.1.79 (versionCode 80) · 31/10/2026

- **Legendas:** novas legendas iniciam em 92% da altura do quadro (margem inferior de aproximadamente 8%); projeto legado mantém posição individual já salva. Atalho Rodapé · 8% e slider vertical no editor de Estilo, aplicáveis também em lote (posição apenas com confirmação explícita).
- **Textos extensos:** autoajuste do tamanho visual proporcional às larguras medidas do texto e da maior palavra, mirando cerca de 3 linhas sem truncar as falas. A mesma regra é usada na prévia Compose e no bitmap exportado Media3, sem modificar o texto original. Conferir fidelidade de tipografia em aparelho.
- **PiP:** slider 0–100% de opacidade por camada (imagem/vídeo), preservado no JSON esquema 4; prévia TextureView/Compose e exportação via compositor Media3 com alfa. Projetos antigos usam 100%.
- **Captura:** tocar na câmera pausa o player ANTES de fixar o tempo de captura; PNG continua sendo do vídeo original sem PiP/legendas. O indicador mostra o instante capturado e o vídeo permanece pausado.
- **Timeline:** rótulos dos vídeos compactos (Clipe 1, 2…), sem alterar títulos reais dos arquivos nem referências persistidas.
- **Compatibilidade:** exportação, assinatura, cinco APKs do workflow, edição offline, Gemini TTS e schemaVersion 4 preservados. Compilação Android e testes visuais/MP4 reais pendentes.

## v0.1.78 (versionCode 79) · 31/10/2026

- **Legendas → Salvar:** persiste o projeto e retorna ao editor de vídeo do mesmo projeto, não à página inicial. Se a gravação falhar, permanece nas legendas e mostra o erro existente.
- **Editar todas:** ação ao lado do SRT, painel global com abas Estilo/Animação, ajustes avançados recolhidos, confirmação de alteração e botão Desfazer (apenas enquanto o lote atual ainda corresponder ao projeto). Preserva textos, IDs e horários.
- **Edição individual:** mantém texto e tempos e reorganiza os controles visuais; sliders com margens e ações no rodapé do diálogo. Prévia simulada do estilo, com animação do conteúdo e sem mudança de mídia.
- **Reconhecimento Vosk:** relatório do estágio de carregamento do modelo (indeterminado), progresso baseado nos timestamps decodificados por clipe (0–98%), organização dos resultados e cronômetro real. Não estima tempo restante nem inventa progresso durante a abertura do modelo.
- **Compatibilidade:** Vosk offline, SAF/SRT, Media3, Gemini TTS, exportação, assinatura e `schemaVersion=4` preservados. Build Android e validação da aparência/performance em dispositivo físico pendentes.

## v0.1.77 (versionCode 78) · 31/10/2026

- O gerenciamento Gemini passou do formulário embutido para Configurações > Google Gemini, com navegação de retorno a Configurações ou Áudio, botões organizados 2×3 e teste/Salvar separados.
- A Narração com IA não expande mais a tela Áudio: diálogo modal adaptável com rolagem interna, fechamento impedido durante geração e seletor de vozes pesquisável por nome/código da API com LazyColumn.
- Mantidos avisos de possível consumo de cota, confirmação, cancelamento, geração WAV, timeline e armazenamento existentes. A descrição original em inglês da voz não ocupa mais o formulário.
- Adicionados testes de pesquisa de vozes e rotas. Não houve migração de projeto; `schemaVersion=4` mantido.
- Verificações locais não substituem compilação Gradle, teste com Gemini real ou exame da aparência em dispositivo físico.

## v0.1.76 (versionCode 77) · 31/10/2026

- Integração opcional do Gemini TTS via `/v1beta/interactions`: geração WAV, modelos Flash/Lite 3.8, vozes consultadas na conta, estilo estruturado e prévia gerada com aviso de cota.
- Nova seção em Configurações com chaves AES-256-GCM no Android Keystore, SAF, pré-visualização mascarada, confirmação de cópia/exportação/remoção e teste de serviço TTS efetivo.
- Faixa Gemini persistida como `AudioTrack` padrão para corte, volume, timeline e composição Media3; schemaVersion 4 preservado. Suporte WAV precisa de validação física na exportação final.
- Tratamento de falhas de rede/cota/autorização, limite de resposta, validação de RIFF, arquivo temporário atômico, cancelamento sem retry automático e mensagens de privacidade.
- Testes unitários da importação, WAV, classificação de erros e serialização, além da validação estrutural; build Android pendente neste ambiente.

## v0.1.75 (versionCode 76) · 31/10/2026

- Adicionada tela Áudio do clipe (importar via SAF, pré-escuta, volume, aplicar/remover). Vínculo opcional por ID e âncora na fonte; prévia e MP4 usam a posição resolvida dinamicamente.
- Preservados schemaVersion=4, trilhas antigas e arquivos originais. Build Android completo pendente.

## v0.1.74 (versionCode 75) · 31/10/2026

- **Tela Áudio redesenhada:** três cartões com Mixagem do projeto, Adicionar áudio e Extrair áudio de vídeo, mantendo seleção SAF, volume original, trilhas, música legada, corte, confirmação de exclusão, pré-escuta e exportação M4A/MP4 AAC.
- **Controles compactos:** ações de origem Projeto inteiro/Clipe atual/Vídeo externo, seleção de galeria e arquivos para vídeo, formatos e salvar/salvar e usar. Rótulos de arquivo resumidos apenas para exibição, sem alterar títulos persistidos.
- **Fluxo real preservado:** ao escolher áudio, o app já o adiciona ao projeto; a tela não mostra botão fictício para aplicá-lo novamente. O seletor de áudio continua SAF, sem copiar URIs transitórias.
- **Compatibilidade:** `schemaVersion=4`, mídias, PiP, legendas, Vosk, captura de quadros e exportação inalterados. Testes Android/CI e aparência no aparelho pendentes.

## v0.1.73 (versionCode 74) · 31/10/2026

- **Velocidade da prévia:** botões − e + percorrem 14 valores entre 0,1× e 6×, com seleção direta ao tocar no valor e restauração para 1×. Apenas prévia; não altera a exportação nem o tempo do projeto.
- **Controles de quadro:** os comandos ±5 s e os quatro botões de ajuste fino foram substituídos por quadro anterior/próximo (busca por tempo aproximado). Eles pausam o player e utilizam FPS nominal do clipe quando disponível, com fallback estimado de 30 FPS.
- **Limitação:** Media3 não garante salto exato entre quadros nem desempenho em 0,1× ou 6× em qualquer aparelho; conferir em testes físicos e com vídeo de FPS variável.
- **Compatibilidade:** botão de câmera PNG, MediaStore, PiP, legendas, formatos de projeto `schemaVersion=4`, Vosk e parâmetros de exportação preservados.
- **Testes:** controles de velocidade e cálculo temporal em JVM, validador e análise sintática; build Android/CI pendente.

## v0.1.72 (versionCode 73) · 31/10/2026

- **Prévia com velocidades extras:** menu compacto em grade, agora de 0,25× até 5× (incluindo 2,5×, 3×, 4× e 5×). A velocidade altera apenas a reprodução, nunca o MP4.
- **Capturar quadro do vídeo:** ícone de câmera ao lado da velocidade extrai o quadro original na posição da timeline, respeitando cortes e limites entre clipes. Salva PNG em `Pictures/UaiCortes` pela galeria do Android 10+.
- **Qualidade e privacidade:** extração local em IO, sem capturar botões da interface; o PNG representa o vídeo de origem, sem PiP, legenda ou transições sobrepostas. Salvar outro quadro não altera o projeto nem os arquivos de vídeo.
- **Falhas seguras:** erro explícito quando a URI perdeu permissão ou o quadro não é extraível; MediaStore apaga entradas parciais; bloqueio de toques repetidos durante a gravação. Em Android 8/9, pasta externa específica do aplicativo é usada.
- **Testes:** novos casos de instante original, cortes, fronteiras, nomes de arquivos e velocidades. Build Android/CI e captura real em aparelho ainda pendentes. `schemaVersion=4` preservado.

## v0.1.71 (versionCode 72) · 31/10/2026

- **Correção de status do Vosk:** a 0.1.70 deixava a última etapa `Descompactando e verificando o modelo...` visível mesmo depois de `installed=true`, devido à mensagem `stage` persistida na tela. Agora o estado transitório é limpo na conclusão e nunca exibido quando o modelo já está instalado.
- **Teste de regressão:** quatro casos JVM distinguem instalação reconhecida, operação em curso, falha e cancelamento. Orientação para testar a transcrição real após instalar.
- **Preservação:** nenhuma alteração no modelo Vosk, arquivos instalados, projetos (`schemaVersion=4`), editor ou exportador. Os três botões continuam na mesma linha quando o modelo não está instalado.
- **Pendente:** confirmação em APK Android e execução real do Vosk nativo; a detecção de arquivos não comprova transcrição funcional.

## v0.1.70 (versionCode 71) · 31/10/2026

- **Causa comprovada pelo diagnóstico, vídeo e ZIP real:** `vosk-model-small-pt-0.3.zip` contém `final.mdl`, `mfcc.conf` e grafos na raiz (layout Vosk legado). O instalador 0.1.69 exigia exclusivamente `am/final.mdl` + `conf/model.conf`, rejeitando o arquivo oficial em `extractAndInstall`.
- **Correção:** `VoskModelLayout` identifica modelos legados e modernos, exigindo modelo acústico, configuração MFCC e grafo não vazios. Os caminhos nativos são preservados, sem alterar estrutura original nem criar configuração artificial. Mesma validação para download interno, importação SAF e detecção de modelo instalado.
- **Interface:** três botões de instalação em uma única linha (Baixar, Navegador, Importar ZIP), com ícones e rótulos compactos. Cancelamento e progresso continuam separados.
- **Testes:** três novos casos de layout; inspeção do arquivo ZIP real enviado; validador estrutural atualizado. Sem build Android completo/teste do Vosk nativo em aparelho nesta entrega.
- **Compatibilidade:** `schemaVersion=4`, dados de projetos, PiP, exportação e reconhecimento offline mantidos.

## v0.1.69 (versionCode 70) · 31/10/2026

- **Vosk:** alternativa ao download interno: abrir o endereço oficial no navegador e importar o ZIP com seletor Android (SAF), sem extração manual.
- **Instalação:** regras de tamanho, verificação de arquivos essenciais, descompactação com bloqueio de caminhos inseguros, limpeza de arquivos temporários e exclusão mútua entre instalações.
- **Progresso:** bytes reais recebidos e percentual quando o servidor informa tamanho; indicador indeterminado quando não informa.
- **Erros:** diferenciar timeout, falha de DNS, HTTP inválido e ZIP incorreto, sem alegar que a falha original no aparelho foi reproduzida.
- **Preservação:** transcrição offline, `schemaVersion=4`, projetos, PiP e qualidade de exportação inalterados.
- **Validação:** parser Kotlin, regras isoladas de progresso e estrutura de projeto; download real, build Android e uso com Vosk dependem de testes no dispositivo/CI.

## v0.1.68 (versionCode 69) · 31/10/2026
- **Novidades na primeira abertura:** uma tela visual apresenta as atualizações e aparece automaticamente somente se houver mudanças ainda não vistas. A confirmação é armazenada localmente por assinatura do conteúdo das notas, não apenas por número de versão.
- **Informações sincronizadas:** `AppReleaseNotes` é a fonte única usada por `Sobre` e `Novidades`, incluindo as atualizações recentes. Removido o texto antigo da tela Sobre e corrigida a exibição do `versionCode`.
- **Acesso manual:** Configurações > Novidades e Sobre > Ver novidades permitem consultar a lista sem forçar nova abertura automática. Ao tocar em Continuar ou Voltar, a leitura é registrada.
- **Regressões:** testes verificam identidade das listas, reapresentação apenas após novidades reais, restauração da rota e retorno correto. `schemaVersion=4`, vídeos, projetos, exportação, PiP e Vosk preservados.
- **Pendente:** build Gradle/Android, teste no aparelho, rotação e conferência visual da tela nos temas claro e AMOLED.

## v0.1.67 (versionCode 68) · 31/10/2026

- Tela inicial: lixeira direta por projeto e ação de excluir todos com confirmação da quantidade completa; exclusão atômica e sem apagar vídeos originais/MP4s exportados.
- Criação de projeto pede nome antes de selecionar os vídeos; o nome persistido é usado como sugestão do MP4 (sanitização de nome inválido) e continua editável em Exportar.
- Galeria: a busca grande virou uma lupa opcional no cabeçalho; aba única Vídeos/Imagens não aparece quando o filtro já fixa esse tipo; filtros de pasta e SAF continuam acessíveis.
- Legendas automáticas: configuração/instalação Vosk transferida para Configurações > Reconhecimento de voz, com progresso numérico percentual, etapas de verificação/extração, cancelamento e mensagens de erro. Link oficial preservado, teste de download físico pendente.
- PiP: seletor de aplicação temporal aos clipes reais do projeto, projeto inteiro ou cursor ao final; vídeos PiP não são estendidos além da duração original sem suporte de loop. Dois campos compactos e uma única barra de intervalo substituem sliders duplicados.
- Testes e validador ampliados. `schemaVersion=4` preservado. Compilação Android e validação em dispositivo pendentes.

## v0.1.66 (versionCode 67) · 30/10/2026

- **Rascunhos seguros do Animar Imagens:** codec separado valida URIs e opções e distingue primeira execução, rascunho íntegro, backup recuperável e cadastro danificado.
- **Backup consistente:** antes de salvar, a última versão JSON válida permanece como backup; se ambas as cópias estiverem danificadas, a gravação é bloqueada até o usuário confirmar um reinício explícito. Não apaga imagens originais.
- **Permissões e fotos indisponíveis:** verificação de leitura local em `Dispatchers.IO` antes da exportação; referências sem acesso permanecem no rascunho, são comunicadas na tela e só são removidas por ação explícita.
- **Testes de regressão:** novos casos para formato anterior, JSON inválido, ordem, preservação do backup e acesso a fotos. Acessos persistentes do SAF continuam solicitados pelo seletor Android.
- `schemaVersion=4` preservado, sem mudanças no Media3, bitrate ou no catálogo de projetos. Build Android completo, testes JUnit Gradle e testes reais de permissão no aparelho ainda pendentes.

## v0.1.65 (versionCode 66) · 30/10/2026

- **Persistência e recuperação de projetos:** o cadastro de projetos agora distingue uma instalação nova de uma chave principal ausente/corrompida. A cópia de segurança é examinada antes de assumir que não há projetos.
- **Escrita protegida:** o backup recebe somente o último JSON previamente validado; IDs em branco ou duplicados não podem ser persistidos. Dados ilegíveis continuam bloqueando gravações destrutivas.
- **Aviso de armazenamento:** o aplicativo informa quando recuperou o backup ou quando ambas as cópias estão ilegíveis. Não apresenta um erro de leitura silenciosamente como se os projetos tivessem sido excluídos.
- **Falhas de salvamento:** a interface informa o erro e não executa automaticamente o comando de voltar para a tela inicial quando a gravação falha; a exclusão também informa erro sem esconder a falha do usuário.
- `schemaVersion=4` preservado. Nenhuma migração de projetos nem alteração nos motores de exportação. Compilação Android completa e testes físicos permanecem pendentes.

## v0.1.64 (versionCode 65) · 30/10/2026

### Refatoração de reprodução e timeline
- Introduzido `TimelinePlaybackCoordinator` com coordenadas globais e locais reutilizáveis.
- Centralizados os caminhos de seek do editor e da prévia, com avanço/retrocesso entre clipes.
- Arrasto do slider protegido contra a atualização periódica que causava saltos no cursor.
- `EditorPlaybackViewModel` retém apenas velocidade da prévia e ID do projeto; player permanece sob ciclo de vida da tela.
- `Player.Listener` notifica imediatamente play/pause; a posição recuperada é aplicada após preparar a fila de mídias.
- Novos testes JVM para bordas e restauração de estado.
- Build Android, CI integral e testes reais pendentes.

## v0.1.63 (versionCode 64) · 30/10/2026
- **Animar Imagens modularizado:** `ImageAnimationTaskViewModel` assume o motor Media3, progresso, cancelamento e confirmação do MP4; a interface só apresenta estados e opções.
- **Reaproveitamento:** mesmo `ExportTaskCoordinator` do exportador principal com tickets de tentativas, fases PREPARING/RENDERING/SAVING e progresso numérico somente quando o Transformer o informa.
- **Concorrência:** instância própria de `ImageAnimationEngine` para cada tentativa, para impedir que a limpeza de um preparo cancelado remova arquivos de outra tentativa. A limpeza durante decodificação é adiada até terminar a operação atual.
- **Rotação:** ViewModel mantém a exportação durante recriação da Activity e as opções visuais são salváveis com valores simples; não recupera trabalho após morte do processo.
- **Compatibilidade:** animação continua offline, PNG temporário sem perdas, H.264 720p/1080p, limite de 80 imagens, formato `schemaVersion=4`, integração com editor e cinco APKs por ABI preservados.
- **Pendente:** build Android Gradle/CI, testes de rotação, cancelamento, salvamento e qualidade MP4 em dispositivo físico. Testes unitários da máquina de estados não provam processamento real.

## v0.1.62 (versionCode 63) · 30/10/2026
- Extraiu estado e motor de exportação da composição para `ExportTaskViewModel` com `viewModelScope`.
- Criou `ExportTaskCoordinator` puro para andamento, sucesso, falha, cancelamento, identificação de tentativa, tempo e progresso.
- Ignora callbacks de tentativas antigas e bloqueia cancelamento após iniciar cópia para a galeria.
- Tornou a cópia `GalleryVideoSaver` indivisível em relação ao cancelamento; limpeza e notificação do resultado no dispatcher principal.
- Testes Kotlin/JVM para fases, cancelamento, progresso não inventado, falhas e callbacks atrasados.
- Preserva schema 4, H.264/AAC, bitrates e demais funções. Compilação Android, lint e testes reais de exportação pendentes.

## v0.1.61 (versionCode 62) · 30/10/2026
- **Refatoração incremental de arquitetura:** `EditorProjectViewModel` passa a manter a lista de projetos, o projeto ativo e a ID de sessão durante a recriação da interface. A `MainActivity` apenas consome snapshots e encaminha comandos.
- **Restauração limitada e segura:** `SavedStateHandle` recebe somente o ID do projeto. Ao recriar o ViewModel após perda do processo, a versão atual do projeto é carregada novamente de `ProjectStore`. Não serializa JSON, mídia, bitmaps ou jobs no Bundle.
- **Operações testáveis:** `ProjectSessionCoordinator` encapsula carregar, salvar, abrir, excluir e anexar clipes. Snapshots são publicados apenas depois que o repositório conclui a operação. Projeto excluído/inexistente volta a ID vazia, sem criar dados fictícios.
- **Compatibilidade:** `schemaVersion=4`, regras PiP, legendas, importação cancelável, qualidade H.264/AAC da 0.1.56 e cinco APKs por ABI preservados.
- **Validação:** testes isolados do coordenador e análise estática/sintática; build Android completo, lint Gradle e testes em aparelho ainda exigem CI. Não afirmar retomada de importação/exportação após morte de processo.

## v0.1.60 (versionCode 61) · 30/10/2026
- **Refatoração do ciclo de vida das importações:** novo `MediaTaskCoordinator`, independente de Activity/Context, que coordena vídeos, áudio e PiP usando o escopo Compose existente. Encerramento da composição cancela a leitura pendente; não é um serviço em segundo plano nem um ViewModel persistente.
- **Cancelar importação:** diálogo da leitura de metadados oferece cancelamento, liberando a interface imediatamente. As leituras no Android continuam cooperativas: operações bloqueantes do sistema podem terminar posteriormente, mas seu resultado cancelado não deve ser aplicado.
- **Troca de destino segura:** ao mudar projeto, tela ou ferramenta, a tarefa antiga é cancelada. Tickets únicos impedem que um `finally` de tarefa cancelada libere outra importação iniciada em seguida, inclusive da mesma categoria.
- **Tratamento de falhas:** cancelamentos não são reportados como erros; falhas reais mantêm diagnóstico e mensagem genérica. As escritas do projeto continuam no dispatcher principal após a leitura de metadados em IO.
- **Compatibilidade:** schemaVersion 4, projetos, Media3 e qualidade solicitada de exportação (720p/8 Mb/s; 1080p/18 Mb/s) inalterados.
- **Verificações:** cinco testes Kotlin/JVM isolados da camada de tarefas passaram, incluindo cancelamento, reentrada e erro real. Testes Gradle/Android completos e validação em aparelho ainda pendentes.

## v0.1.59 (versionCode 60) · 30/10/2026
- **Refatoração de tarefas de mídia:** `MediaImportTasks.kt` concentra as consultas a metadados de vídeo, áudio e PiP em `Dispatchers.IO`; evita que os seletores SAF bloqueiem a thread principal com leitura de duração e nomes.
- **Concorrência e destino:** `MediaTaskGate` permite apenas uma importação ativa; `MediaTaskOrigin` rejeita resultados quando projeto, tela ou ferramenta já mudaram. Áudio e PiP conferem ainda o projeto em memória antes de salvar.
- **Importação inválida:** o PiP não recebe duração fictícia quando os metadados não puderem ser verificados, e não é inserido quando a posição não tem espaço restante no vídeo.
- **Interface mais enxuta:** removidos imports obsoletos de `MainActivity.kt`; o estado de importação apresenta diálogo único e continua restrito ao escopo de corrotinas do Compose.
- **Compatibilidade:** sem mudanças em `schemaVersion=4`, projetos, formatos de mídia, permissões, exportadores ou opções de 720p/1080p e bitrate da v0.1.56.
- **Validação:** testes unitários novos para exclusão mútua e rejeição de resultados tardios, compilação Kotlin/JVM isolada incluindo o adaptador com corrotinas, validação estrutural e integridade do ZIP. Gradle/Android completo, regressão de UI e qualidade real do MP4 continuam pendentes.

## v0.1.58 (versionCode 59) · 30/10/2026
- **Correção do workflow 51:** adicionados em `MainActivity.kt` os imports `kotlinx.coroutines.launch` e `kotlinx.coroutines.CancellationException`, ausentes na importação de vídeos assíncrona da v0.1.57.
- **Compilação:** resolve as referências não encontradas que bloquearam `:app:compileDebugKotlin` e `:app:compileReleaseKotlin`. O erro de uso de `withContext` fora da coroutine era consequência da referência `launch` não resolvida.
- **Prevenção de regressão:** `tools/validate_source.py` verifica a presença única desses imports, `Dispatchers.IO`, `withContext` e a propagação de `CancellationException`.
- **Compatibilidade:** mantidos `schemaVersion=4`, bitrates/qualidade da v0.1.56, importação assíncrona e os controles atuais de projeto, sem mudar interfaces ou exigir novas permissões.
- **Validação:** inspeção dos logs do workflow 51, validação estrutural, sintaxe Kotlin e compilação isolada de API de corrotinas; Gradle/Android completo e testes em aparelho permanecem a confirmar.

## v0.1.57 (versionCode 58) · 30/10/2026
- **Correção do workflow 50:** teste `EditorWorkspaceStateTest.deletedSelectionsAreRevalidatedAgainstPersistedProject` corrigido: dois clipes possuem uma junção de índice **0**, não 1. Testes adicionais cobrem zero, um, dois e três clipes e revalidação após exclusão.
- **Transições robustas:** `TransitionBoundaryPolicy` centraliza o limite de junções; a prévia usa índice validado mesmo quando o projeto muda mantendo o ID. O editor atualiza a seleção na alteração do projeto, não apenas do identificador.
- **Importação responsiva:** metadados de vídeos importados são consultados em `Dispatchers.IO`; a interface mostra progresso indeterminado, impede duas importações concorrentes e descarta resultado atrasado se o projeto ativo mudou.
- **Mídia inválida:** novas importações usam duração verificada (0 quando indisponível), evitando criar clipes fictícios de 1 segundo. Fallback legado de outros fluxos permanece por compatibilidade.
- **Compatibilidade:** `schemaVersion=4` preservado, qualidade de exportação da 0.1.56 mantida, versionName/versionCode, documentação e workflow sincronizados.
- **Validação:** parser Kotlin, verificação estrutural e execução de testes JVM isolados; Gradle/Android completo e reprodução/exportação reais dependem de CI/aparelho.

## v0.1.56 (versionCode 57) · 30/10/2026
- Exportação de projetos: H.264 com bitrate solicitado de 8 Mb/s (720p) ou 18 Mb/s (1080p), AAC 192 kb/s e fallback do codec mantido.
- Qualidade padrão de novos usuários: 1080p; configurações existentes do usuário são preservadas.
- Animação de imagens: PNG temporário sem perdas (antes JPEG 94), mesmo bitrate de H.264, checagem estimada de espaço para MP4 temporário, cópia MediaStore e imagens preparadas.
- Interface de exportação informa bitrate solicitado e espaço estimado (não garantidos); registro técnico inclui bitrate real reportado pelo Media3.
- Resolução de origem não é recuperada por upscale. Pendente: compilação Android, teste no aparelho e comparação visual objetiva com MP4 da 0.1.55.

## v0.1.55 (versionCode 56) · 30/10/2026
- **Operações de projeto reutilizáveis:** `ProjectController` e `ProjectRepository` separam carregar, abrir por ID, salvar, criar/estender e excluir do código de interface. Atualização do estado acontece somente depois de `save/delete` no armazenamento; o backup e o JSON `schemaVersion=4` continuam em `ProjectStore`.
- **PiP desacoplado:** `OverlayOperations` concentra inserção sequencial, limite de quatro camadas, substituição por ID, cálculo de colisões, autorização explícita para sobrepor e remoção/seleção seguinte. A Activity mantém avisos, confirmação e chamadas de gravação.
- **Importações compartilhadas:** `MediaImportController` unifica consultas locais de metadados e pedidos de permissão SAF. `MediaImportRules` trata seleção única de imagens (máximo 80), clipes válidos e faixa de áudio dentro do projeto; entrada pela galeria e documentos utiliza o mesmo fluxo.
- **Testes e limites:** adicionados testes JUnit de projetos, PiP e importação, validação estrutural e compilação Kotlin/JVM isolada de regras. Não houve build Gradle/Android nem teste de exportação ou interface em aparelho.
- Documentação: `docs/ARQUITETURA_REFATORACAO_0.1.55.md` e `docs/VALIDATION_0.1.55.md`.

## v0.1.54 (versionCode 55) · 30/10/2026
- **Continuação da refatoração:** `EditorWorkspaceState` agora utiliza `rememberSaveable` e salva uma representação compacta (rota, seleções e posição), sem incluir JSON, vídeos ou fotos no `SavedStateRegistry`.
- **Projeto por ID:** o projeto ativo é localizado novamente no `ProjectStore` após recriação da Activity; IDs removidos e posições fora da duração são normalizados, sem alterar a estrutura `schemaVersion=4`.
- **Navegação reutilizável:** `EditorNavigation` concentra rotas, ação Voltar e tratamento de restauração sem projeto. O retorno entre ferramentas mantém as mesmas regras da versão anterior.
- **Fluxos de seleção:** origem/ação da galeria, destino de animação, URIs selecionadas de imagens (máximo 80) e vídeo para extração são salvos quando o Android fornece estado de instância.
- **Testes:** novos testes JUnit para snapshot, restauração sem projeto, seleções inválidas e navegação. Testes Gradle/Android e rotação física ainda pendentes; exportação e transcrição em andamento não são retomadas automaticamente.
- Documentação: `docs/ARQUITETURA_REFATORACAO_0.1.54.md` e `docs/VALIDATION_0.1.54.md`.

## v0.1.53 (versionCode 54) · 30/10/2026
- **Refatoração de base:** `MainActivity.kt` reduzida à inicialização/coordenação. Telas do editor, cortes, áudio, PiP, transições, legendas, exportação, marca d'água e timeline extraídas para arquivos Kotlin dedicados, sem mudar os fluxos visuais.
- **Estado compartilhado:** `EditorWorkspaceState` concentra navegação e seleções transitórias de clipes, PiPs, áudio, legendas e posição. Rascunhos e projetos continuam em seus armazenamentos próprios; não foi adicionada restauração automática de processo.
- **Exportação reutilizável:** `GalleryVideoSaver` publica MP4s de ambos os motores no MediaStore e só devolve a URI após concluir a cópia; elimina implementação duplicada e remove o item parcial ao falhar.
- **Segurança dos projetos:** `ProjectCollectionCodec` e `ProjectStore` preservam cópia anterior válida e bloqueiam `save/delete` se o cadastro e a cópia estiverem ilegíveis, evitando substituir dados por lista vazia. JSON de projetos continua `schemaVersion=4`.
- **Verificações:** validador estrutural atualizado e testes unitários acrescentados para codec/recuperação; sintaxe Kotlin inspecionada. Build Android/Gradle, testes unitários completos e verificação de MP4 em aparelho permanecem pendentes.
- Veja `docs/ARQUITETURA_REFATORACAO_0.1.53.md` e `docs/VALIDATION_0.1.53.md`.

## v0.1.52 (versionCode 53) · 30/10/2026
- **Animar Imagens:** controles padronizados e compactos; botão Salvar edição no cabeçalho para persistir fotos e configurações como rascunho local.
- Ao voltar, alterações não salvas geram confirmação; importações e reordenação preservadas. Voltar não permanece desativado após finalização.
- Tela de vídeo salvo reutilizada do editor principal, com Abrir, Compartilhar, Editar com áudio/legendas/PiP e Fechar; MP4 gerado continua sendo adicionado como clipe editável normal.
- Nova regra testável de alterações de rascunho e validação estrutural ampliada. Sem migração dos projetos existentes, sem dependências adicionais e sem envio de mídia à nuvem.
- Build Android e testes físicos ainda pendentes.

## v0.1.51 (versionCode 52) · 30/10/2026
- Animar Imagens: corrigido o NPE do `LinearProgressIndicator` removendo `progress!!` e usando percentual imutável limitado a 0–100%, barra indeterminada quando ausente.
- Prévia com área compacta de 238 dp e proporção preservada, cartões menores, nomes curtos e controles **Ajustes/Animar** lado a lado.
- Alça de arrastar reposiciona a foto durante o gesto e permite rolagem nas bordas, mantendo botões subir/descer/remover e ordem da galeria.
- Identidade Uai Cortes no progresso, mantendo Media3 apenas no código e nos diagnósticos.
- Novo teste unitário do progresso; compilação Android, exportação MP4 e testes de gesto no aparelho ainda pendentes.

## v0.1.50 (versionCode 51) · 30/10/2026
- **Animar Imagens offline** no menu inicial e nas ferramentas do editor: seleção múltipla na galeria, reordenação por arrasto e setas, movimentos de câmera automáticos, prévia e exportação MP4 H.264 com Media3.
- Ajustes opcionais de intensidade, duração (3/5/8s), formato (9:16/16:9/1:1), 720p/1080p e transições (corte/preto/branco/auto). Padrões: 5s, 9:16, 720p, 30 FPS.
- Salvamento no MediaStore, cancelamento, progresso quando disponível, posterior inclusão na timeline com áudio e efeitos; sem rede ou novo modelo.
- Segurança: cópias temporárias, fotos originais preservadas, sem alegação de animação corporal por IA ou paralaxe 2.5D. Veja `docs/ANIMAR_IMAGENS.md`.
- Compatibilidade: projetos antigos e schemaVersion 4 preservados. Compilação e testes de exportação no Android pendentes.

## v0.1.49 (versionCode 50) · 27/10/2026
- **Legendas automáticas offline (Vosk)**: nova tela acessível em Legendas → Auto; tutorial integrado, download opcional do modelo pequeno de português (~31 MB) no site oficial, decodificação de áudio dos vídeos, respeito a cortes e tempo de projeto, prévia/revisão individual antes de aplicar. Limite inicial de 10 minutos por operação; sem upload de áudio. Modelo extraído com proteção contra caminhos inválidos e limite de tamanho. A qualidade do reconhecimento depende da fala e do ruído; compilação e teste em Android ainda pendentes.
- **Transições de movimento**: Zoom cinematográfico, Recuo suave e Deslocamento lateral são efeitos de transformação por quadro Media3, acompanhados de prévia ilustrada. Não são crossfade nem dissolvência real entre clipes.
- **Projetos**: miniatura do primeiro quadro por projeto (com fallback) e confirmação explícita antes de excluir; vídeos originais não são apagados.
- Compatibilidade: schemaVersion 4 preservado; versionCode 50 e release dos cinco APKs pelo workflow. Não declarar APK/ASR testado até build real e teste em aparelho.

## v0.1.48 (versionCode 49) · 27/10/2026
- **Marca d'água em texto:** novo cartão no carrossel inferior do editor, com tela de edição própria, prévia do vídeo, texto até 80 caracteres, três cores, tamanho 12–36 e opacidade de 10% a 100%.
- Nove posições no quadro, usando coordenadas normalizadas do vídeo. **Aplicar** salva no projeto e volta ao editor; **Remover** apaga só a marca, sem tocar no texto das legendas ou nos arquivos originais.
- A marca ocupa a duração inteira do projeto, em camada separada das legendas SRT/animadas. A prévia do editor, a prévia de exportação e o Media3 Transformer receberam a mesma camada; projetos antigos sem `textWatermark` continuam válidos no schemaVersion 4.
- **Legendas automáticas:** estudadas as alternativas offline (whisper.cpp, Vosk) e online, mas transcrição automática ainda não foi implementada nesta versão. Exige decodificação de áudio, marcação temporal, escolha do modelo, revisão e controle de desempenho.
- Testes Kotlin isolados e validação estrutural incluídos. Compilação Android completa e conferência de MP4 em aparelho permanecem pendentes.

## v0.1.47 (versionCode 48) · 28/10/2026
- **Galeria interna:** corrigida a falha `The coroutine scope left the composition`. Corrotinas canceladas por troca de tela/permissão são propagadas e deixam de aparecer como erro de carregamento da mídia.
- Indicador de carregamento não é encerrado por consulta antiga cancelada; troca de permissões limpa o estado de erro. Em falhas reais, exibe **Tentar novamente** e **Procurar no Android**, sem mostrar caminhos de mídia ou detalhes internos.
- Falhas reais de consulta e permissão registradas no Diagnóstico (quando ativado), com mensagens genéricas na interface. O cabeçalho TXT passa a ler a versão real instalada, em vez de exibir sempre 0.1.41. Preservados filtros, miniaturas, seleção múltipla, SAF e projetos existentes.
- Diagnóstico TXT (cujo cabeçalho era fixo em 0.1.41, independentemente da versão instalada) também mostrou `SecurityException` no player do editor; esse incidente é distinto da falha de ciclo de vida da galeria. Pode exigir nova seleção da mídia e autorização no Android.
- Testes unitários de mensagens de erro, regressão estrutural de cancelamento, versionamento sequencial. Build Android e teste real de concessão/revogação de permissões permanecem pendentes.

## v0.1.46 (versionCode 47) · 27/10/2026
- Transições: nove opções, mantendo Corte direto, Fade preto e Fade branco e adicionando Flash branco, Flash preto, Fade azul, Fade dourado, Preto e branco e Sépia. Usam RgbMatrix do Media3 por quadro, sem sobrepor sequências de vídeo.
- Nova seleção de efeitos em faixa horizontal, mini prévia animada a partir de quadros dos dois clipes e duração ajustável para efeitos ativos.
- Escolhas são rascunhos até **Aplicar**. Botão inferior **Aplicar** persiste e retorna ao editor; **Aplicar a todas as junções** replica o efeito/duração nos pares adjacentes, sem alterar clipes ou mídia original.
- Sem migração do schemaVersion 4; testes de matrizes e aplicação em lote adicionados. Exportação e aparência física ainda exigem build CI e teste Android.

## v0.1.45 (versionCode 46) · 27/10/2026
- Exportação em tela cheia ao iniciar “Salvar vídeo”, com etapa Preparando/Processando/Salvando, nome do MP4, qualidade e cronômetro.
- Percentual vem de `Transformer.getProgress` somente quando disponível; indicador indeterminado nos demais estados. Sem ETA fictícia.
- Confirmação de cancelamento antes de descartar o MP4 parcial; o projeto e as mídias originais são preservados. Ao copiar para a galeria, o cancelamento é bloqueado para não interromper a gravação.
- Mantida a confirmação em tela cheia **Vídeo salvo!** somente após finalizar o MediaStore. Exportação não foi validada fisicamente neste ambiente.

## v0.1.44 (versionCode 45) · 27/10/2026
- Galeria interna MediaStore integrada às entradas de mídia visual: Novo projeto, Adicionar vídeo, PiP (adicionar/substituir) e vídeo externo para Extração de áudio.
- Filtros Recentes/Vídeos/Imagens conforme finalidade, busca por nome/pasta, pastas, miniaturas sob demanda, seleção múltipla ordenada para clipes e atualização da biblioteca.
- Permissões específicas por tipo, suporte ao acesso parcial do Android 14+, reavaliação ao retomar e seletor do Android preservado para fontes não indexadas. Sem cópia automática de arquivos grandes.
- Projetos continuam em schemaVersion 4; testes unitários de busca/filtros e validação das rotas de importação.

## v0.1.43 (versionCode 44) · 27/10/2026
- Corrigido erro de compilação do workflow **Gerar APK Uai Cortes 37**: `MainActivity.kt` referenciava `savedName` e `savedVideo` na confirmação de exportação sem declarar o estado no escopo de `ExportScreen`.
- Estados `remember(p.id)` adicionados antes do callback `onSuccess`, com `savedVideo` explicitamente tipado como `Uri?`; a confirmação em tela cheia, Abrir, Compartilhar, Voltar ao editor e Fechar permanecem.
- Validação estrutural ganhou regressão específica para essas declarações e sua ordem; sem mudanças nos projetos, legendas, motor Media3 ou formato do MP4. Compilação Android e teste físico continuam pendentes.

## v0.1.42 (versionCode 43) · 27/10/2026
- Exportação de legendas: `TimedSubtitleOverlay` respeita largura máxima de 90% do quadro, com quebras de linha automáticas e tamanho em proporção à largura real do vídeo, em vez de bitmap horizontal ilimitado. Preserve tamanho, posição, efeitos e tempos originais do projeto.
- `TextOverlay.getBitmap` substituído por StaticLayout com largura controlada e cache por quadro; legenda sem texto continua transparente, protegida contra imagem 0×0.
- Sucesso de exportação em tela cheia, somente após salvar no MediaStore, com Abrir, Compartilhar, Voltar ao editor e Fechar; falhas continuam detalhadas na tela de exportação.
- Não foi possível realizar exportação MP4 em aparelho neste ambiente; confirmar fidelidade da prévia e os efeitos no vídeo físico, vertical e horizontal.

## v0.1.41 (versionCode 42) · 21/10/2026
- Nova seção **Configurações > Diagnóstico**, desligada inicialmente e ativada manualmente, para capturar data e hora, tela ativa, toques aproximados (grade de 5%) e ações de navegação/salvamento.
- Captura falhas não tratadas, erros Media3 dos players, código/causa categorizada da exportação, extração de áudio e falhas ao salvar o relatório; logs internos limitados a 512 KiB com rotação.
- Visualização local, exportação TXT pelo seletor SAF, compartilhamento explícito pelo Android Sharesheet com FileProvider e exclusão com confirmação. Nenhum upload automático.
- Relatórios omitem textos digitados, legenda, nomes e URIs de mídia, IDs de projetos e conteúdo de arquivos; incluem modelo do aparelho/Android quando exportados. Modo não substitui o Logcat integral ou testes reais no dispositivo.
- Sem mudanças no schemaVersion 4, algoritmos de corte e codificação, projetos salvos e segredos. Build Android e testes físicos pendentes.

## v0.1.40 (versionCode 41) · 21/10/2026
- Corrigida provável causa do erro de exportação Media3 `ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED` com `width and height must be > 0`: `TimedSubtitleOverlay.getText()` retornava string vazia nos intervalos sem legenda e no primeiro frame de `Letra por letra`. O `TextOverlay` do Media3 tenta criar bitmap com largura zero nesse caso.
- A exportação usa caractere NBSP transparente de largura positiva quando nenhum texto deve aparecer, e define alpha 0 no overlay inativo; nenhum caractere adicional é gravado nos textos ou arquivos SRT.
- Acrescentados testes de regressão para legendas vazias, espaços/quebras de linha e texto visível. Mensagem de erro específica para falha de composição de quadro.
- Preservados corte, áudio, PiP, efeitos, projeto JSON schemaVersion 4 e o comportamento das telas. Compilação Android e exportação em aparelho ainda pendentes.

## v0.1.39 (versionCode 40) · 21/10/2026
- Corrigida configuração de duração de itens Media3 Transformer: não informar a duração *recortada* como se fosse a duração original da fonte em vídeos, PiPs e áudios. A biblioteca lê a duração intrínseca das mídias.
- Exportação: valida leitura das URIs locais e limites dos cortes antes do processamento, verificando espaço mínimo temporário; em caso de falha apresenta orientação mais específica, código Media3 e detalhe da causa quando disponível (também Logcat).
- Prévia Exportar: exibe legendas existentes com animações e estilo, camadas PiP e fades; anteriormente reproduzia somente o vídeo bruto, confundindo o usuário.
- A prévia ainda não garante fidelidade de áudio/mixagem; conferir o MP4 real depois do build e os códigos retornados no aparelho. "Asset loader error" sem código/causa não basta para diagnóstico definitivo.
- Mantidos projetos e esquema JSON 4, sem alterar arquivos originais. Compilação Gradle, exportação real no aparelho e conferência de legendas/PiPs pendentes.

## v0.1.38 (versionCode 39) · 21/10/2026
- Legendas: novo botão compacto **Salvar** na mesma faixa de **Nova** e **SRT**. Salva projeto editável e retorna à Home, não exporta MP4.
- Pop-up: efeitos de revelação lado a lado em carrossel circular horizontal (itens virtualizados), sem coluna longa; o efeito ativo mantém seleção.
- Áudio: seção **Mixagem** inicialmente recolhida para evitar controles de volume de todos os vídeos; cada faixa adicional exibe título, volume e edição em uma linha e abre o slider só quando solicitado.
- Exclusão de faixa de áudio agora pede confirmação e preserva o arquivo de origem; ao excluir uma faixa em edição o rascunho é fechado.
- Mantidos formato, volume e recorte reais já existentes; `schemaVersion=4` permanece. Validar UI, build Gradle, prévia e MP4 em aparelho.

## v0.1.37 (versionCode 38) · 21/10/2026
- Tela Legendas: editor em diálogo compacto com prévia ilustrativa de estilo, texto, início/fim lado a lado, botão Estilo, Animação, Excluir e Salvar.
- Rascunho local: alterações no pop-up apenas são persistidas ao tocar em Salvar. Cancelar descarta as mudanças do diálogo.
- Novas legendas manuais e de SRT usam tamanho inicial de 12 sp; projeto legado sem atributo continua sendo lido com 22 sp para preservar aparência histórica.
- Aplicar a todas permite escolher aparência, posição e animação de forma independente, com confirmação antes da gravação; não altera textos, horários nem IDs.
- Animações offline: letra por letra, frase por frase, destaque progressivo de palavras, pop/zoom, fade e deslizar, com tempo compartilhado entre prévia Compose e exportação Media3. A sincronização é temporal aproximada dentro de cada legenda, não acompanha a fala automaticamente.
- Preserva schemaVersion 4; as novas propriedades opcionais de animação usam defaults compatíveis. Build Android, testes em dispositivo e fidelidade exata prévia/MP4 ainda pendentes.

## v0.1.36 (versionCode 37) · 20/10/2026
- Correção de compilação do workflow "Gerar APK Uai Cortes 31": `searchQuery` era acessada no callback de importação SRT antes de ser declarada em `SubtitleScreen`; a declaração do estado foi movida para antes do importador.
- Adicionada verificação estrutural específica para evitar regressão de escopo/ordem no importador SRT. Preservados busca, legendas, prévia adaptativa, exportação e `schemaVersion=4`.
- Logs confirmaram falha em `:app:compileDebugKotlin` e `:app:compileReleaseKotlin` na linha 2926, após a validação estrutural aprovada. Novo build Android, testes e APKs pendentes de execução no GitHub Actions.

## v0.1.35 (versionCode 36) · 20/10/2026
- Prévia Media3 com dimensões proporcionais ao vídeo em Legendas, Cortar e unir, PiP e Exportar. A altura usa limite compatível com a tela, sem barras pretas externas artificiais ou esticar o arquivo.
- Editor principal, PiP e Exportar acompanham a mudança de proporção entre clipes durante a reprodução.
- Legendas: quebra de palavra mitigada medindo a maior palavra e ajustando fonte apenas na prévia; largura e posição do bloco respeitam os limites do quadro real.
- Conteúdo e estilo persistido da legenda não se alteram; schemaVersion 4, comportamento do exportador, SRT e mídia originais preservados.
- Adicionado `AdaptiveVideoLayout.kt` e testes de dimensões/aspect ratio em `AdaptiveVideoLayoutTest.kt`. Compilação Android e verificação de fidelidade no MP4 ainda pendentes.

## v0.1.34 (versionCode 35) · 20/10/2026
- Legendas: busca rápida de texto independente de maiúsculas e acentos, contagem do resultado e indicação clara de busca sem correspondências. A numeração permanece a da lista completa.
- Prévia fixa ganhou botões de navegação para legenda anterior / seguinte, na ordem temporal, com posição do vídeo sincronizada e sem salto circular involuntário.
- Excluir legenda requer confirmação; Cancelar mantém legenda, texto e horários. Após excluir, a seleção avança para um item existente.
- Nova legenda respeita duração do vídeo mesmo no último milissegundo; caso vídeo esteja vazio, o botão de criação é desabilitado.
- Importador SRT rejeita arquivos acima de 250 mil caracteres, evitando importação silenciosamente incompleta.
- Adicionados helper puro `SubtitleListNavigation.kt`, testes JUnit, checks estruturais e documentação. `schemaVersion=4` mantido; compilação Android e testes físicos pendentes.

## v0.1.33 (versionCode 34) · 20/10/2026
- Legendas: prévia do vídeo fixa no topo, com reprodução Media3 restrita ao trecho selecionado e posicionamento por arrasto no próprio quadro.
- Timeline única, horizontal e virtualizada por blocos de 10 segundos. Indicadores utilizam intervalos absolutos, inclusive quando legendas se sobrepõem.
- Lista compacta LazyColumn: só uma legenda expande por vez, com texto, tempos numéricos e ajuste de alças; estilos e posição avançada recolhidos inicialmente.
- Importar SRT, prévia/exportação, IDs e schema 4 continuam compatíveis. Acrescentados testes de janelas temporais.
- Verificações locais de sintaxe/estrutura e lógica; build Android, lint e testes visuais em aparelho pendentes.

## v0.1.32 (versionCode 33) · 20/10/2026
- Cortar e unir: faixa horizontal de trechos com uma miniatura carregada sob demanda por clipe, parte numerada, horários e borda azul na seleção. Toque no cartão para selecionar e rolagem automática até a parte ativa.
- Dividir agora seleciona automaticamente a parte à direita, posicionando o cursor na divisão em milissegundos da fonte. O ponto e ID do trecho ficam gravados nos dados opcionais do projeto, restaurados ao reabrir, sem mudar `schemaVersion=4`.
- Excluir escolhe o trecho vizinho; Desfazer restaura também os dados do último corte; ao dividir, as transições já presentes na fronteira original são preservadas na nova fronteira externa.
- Testes JUnit `TrimTimelineEditTest` para sequência de cortes, seleção, exclusão, transições, JSON legado e pontos inválidos. Compilação Android e inspeção visual em celular continuam pendentes.
- Gradle, workflow, script de cinco APKs, verificador estrutural e tela Sobre sincronizados em 0.1.32 / 33.

## v0.1.31 (versionCode 32) · 20/10/2026
- Ação “Salvar e voltar à Home” retirada de acima das trilhas e convertida em cartão **Salvar** no carrossel de ferramentas do editor.
- Barra inferior com rolagem horizontal cíclica, acessível à esquerda e à direita. Cartões de Cortar, Unir, PiP, Áudio, Transição e Legendas preservados.
- Em Cortar e unir, **Salvar** aparece após Excluir na faixa horizontal rolável, persistindo o corte pendente quando válido e retornando ao início. Erros de intervalo não fazem perder a edição.
- `TrimSaveLogic` criada para tornar a aplicação do corte testável; adicionado `TrimSaveLogicTest` com cenários válidos, inválidos e inalterados.
- `versionName=0.1.31` e `versionCode=32` alinhados ao workflow, script de empacotamento, Sobre e verificador estrutural.
- Sem exportação automática. Gradle/Android CI e validação real de interface ainda pendentes.

## v0.1.30 (versionCode 31) · 19/10/2026
- Corrigido erro real de compilação do workflow 26: `Unresolved reference 'PathJoin'` em `MainActivity.kt:53/2594`. Import e chamada substituídos por `androidx.compose.ui.graphics.StrokeJoin` / `StrokeJoin.Round`.
- Mantidos os efeitos de contorno existentes e todas as funcionalidades da v0.1.29, sem alterar o esquema do projeto.
- Gradle, workflow, script dos cinco APKs, Sobre, verificações estruturais e documentação sincronizados com v0.1.30/versionCode 31.
- Validações locais não substituem `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleRelease`, ainda pendentes em CI.

## v0.1.29 (versionCode 30) · 19/10/2026
- Editor de legendas: sombra de deslocamento diagonal ajustável de 0–6 dp, com opção preta/branca. 0 desativa a sombra.
- Espaçamento adicional entre linhas de 100–160% e alinhamento de parágrafos à esquerda, centralizado ou à direita, preservando projetos antigos.
- Prévia Compose e exportador Media3 atualizados com renderização de sombra por `LineBackgroundSpan`, espaçamento por `LineHeightSpan` e alinhamento por `AlignmentSpan`.
- Persistência JSON por legenda, normalização conservadora de campos e cópia de aparência para todas sem alterar conteúdo, IDs, horários ou coordenadas.
- Testes unitários `SubtitlePresentationTest` incluídos. Build Android completo e comparação visual com MP4 continuam pendentes.

## v0.1.28 (versionCode 29) · 19/10/2026
- Editor de legendas: fonte Moderna, Serifada ou Monoespaçada e contorno ajustável (0–4 dp), preto ou branco.
- Prévia Compose atualizada com traço por baixo do preenchimento e renderização Media3 estendida com `TypefaceSpan` e `LineBackgroundSpan` para contorno por linha.
- Persistência individual em `SubtitleCue`, leitura tolerante a projetos legados e cópia de aparência para todas as legendas sem modificar intervalos e conteúdo.
- Novo `SubtitleTypographyTest`; testes Kotlin/Gradle e comparação visual com MP4 no aparelho ainda pendentes.

## v0.1.27 (versionCode 28) · 19/10/2026
- Adicionados cinco estilos de legenda: Clássico, Destaque, Ciano, Limpo e Etiqueta. A escolha altera apenas a aparência da legenda selecionada.
- Seletor de fundo (sem fundo, suave, escuro ou claro) e controle de negrito, com atualização imediata das prévias no editor e sobre o quadro do vídeo.
- Exportação Media3 usa a cor de fundo individual e a opção de negrito via `SpannableString`/`TextOverlay`, sem alterar tempos, IDs, X/Y ou o arquivo original.
- Botão de copiar aparência para todas as legendas, com confirmação e preservação de texto, posição e intervalos.
- Modelo `SubtitleCue` inclui `backgroundColor` e `bold` opcionais na leitura de JSON, mantendo o fundo escuro e o negrito de projetos anteriores. Schema continua v4.
- Novo `SubtitleStylesTest` com verificações de presets, cópia e leitura JSON legada. Build e renderização real no Android pendentes.

## v0.1.26 (versionCode 27) · 19/10/2026
- Tela principal: blocos individuais na pista de legendas abrem diretamente a legenda selecionada por ID.
- Tela Legendas: faixa individual por texto, inclusive com sobreposição temporal; alça esquerda ajusta início, centro desloca mantendo duração e alça direita ajusta fim.
- Rascunho visual durante o arrasto e persistência apenas ao soltar, preservando texto, X/Y, estilo e JSON legado.
- Limites de projeto e proteção de duração mínima de 100 ms para gestos em trechos normais, respeitando legendas SRT antigas menores que isso.
- Adicionados `SubtitleTimelineEdit` e testes unitários de início, fim, deslocamento, overflow e trechos curtos. Build Android e testes físicos pendentes.

## v0.1.25 (versionCode 26) · 19/10/2026
- Melhorada a precisão de navegação no editor com passos de 100 ms e 1 s, limitados ao intervalo do projeto.
- Legendas posicionadas proporcionalmente ao tamanho do vídeo, sem deslocamentos fixos em dp. Prévia usa a âncora inferior central do exportador.
- Nova miniatura da legenda com arrasto e setas para ajustes de posição de 1%; aplica a posição ao concluir o gesto e mantém sliders existentes.
- Seleção de quadro do clipe correspondente ao tempo da legenda, sem alterar mídia original nem a estrutura JSON.
- Incluídos testes `TimelinePrecisionTest`; compilação Android e fidelidade da exportação ainda pendentes.

## v0.1.24 (versionCode 25) · 19/10/2026
- Timeline principal de áudio agora permite arrastar horizontalmente as faixas extras para alterar o início no projeto, persistindo só ao concluir o gesto.
- Toque em uma faixa seleciona e abre o editor de áudio diretamente nela.
- Editor de áudio com alças de recorte visual: esquerda (ajusta origem e tempo de entrada), centro (move), direita (ajusta fim do arquivo). Prévia dos valores até Salvar corte, preservando Cancelar.
- Lógica `AudioTimelineEdit` com testes Kotlin de limites do arquivo/projeto, duração mínima e conversão pixels/tempo.
- Compatibilidade JSON preservada; áudio legado e exportação não foram modificados nesta versão.
- Verificações locais não substituem Gradle/Android e testes em aparelho.

## v0.1.23 (versionCode 24) · 19/10/2026

- Faixas adicionais com entrada e saída no áudio de origem, posição na timeline, controles numéricos/deslizantes e pré-escuta separada.
- Exportação Media3 e prévia do editor respeitam `sourceStartMs`; projetos antigos permanecem compatíveis com origem em 0 ms.
- Limites de corte conservadores (mínimo de 250 ms) e testes de lógica/serialização adicionados.
- ZIP/validação estrutural: a compilação Android e teste físico desta versão continuam pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Pré-escuta de áudio para clipes individuais e vídeos externos, com reprodução, pausa, reinício e limite de recorte.
- Reprodução temporária isolada da timeline e liberação do player ao sair da tela.
- Pré-escuta de projeto inteiro ainda não implementada. Build Android e testes em aparelho pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Prévia animada de transições usando quadros reais extraídos dos clipes adjacentes, com Fade preto, Fade branco e Corte direto.
- Extração de quadros em IO, sem decodificação contínua ou áudio. A prévia não substitui reprodução/renderização integral.
- Mantidas as cinco variantes de APK e a exportação Media3 existente.
- Build Android e testes em aparelho pendentes.

Correção v0.1.22: o workflow 19 compilou Kotlin Debug/Release, executou testes e lint com sucesso, mas falhou no empacotamento porque tools/package_apks.py ainda esperava 0.1.17/versionCode 17. Constantes sincronizadas para 0.1.22/versionCode 23. Compilação e publicação da nova versão pendentes.

## 0.1.22 (versionCode 23) — 19/10/2026
- Corrigida compilação Kotlin do build 18: chamada de `TimelineRow` para a faixa PiP vazia agora passa explicitamente `onClip`.
- Nenhuma alteração na lógica de exportação ou transições.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

## 0.1.18 (versionCode 19) — 19/10/2026
- Extração de vídeo externo via SAF e recorte por início/fim.
- Posição X/Y de legendas persistida no projeto e aplicada à exportação.
- Implementação parcial: prévia de transições, indicadores na timeline e menu de ações ainda pendentes.

## 0.1.16 (versionCode 17) — 19/10/2026
- Correção do Lint do build 16: configuração instável do `PlayerView` isolada em função com opt-in AndroidX explícito; removidas chamadas Media3 instáveis de lambdas Compose.
- Configuração de build ABI splits `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` e universal, sem criar variantes Debug para distribuição.
- Correção de distribuição duplicada: o workflow não escuta mais tags criadas na publicação nem publica o mesmo APK como Actions Artifact. Uma Release com cinco APKs nomeados.
- Validação automatizada de cinco saídas Release, metadados de ABI, assinatura, pacote, targetSdk, versionName e versionCode. Atualizados docs e tela Sobre.
- APK/Android Lint ainda dependem de execução no GitHub Actions; testes físicos por arquitetura pendentes.

## 0.1.15 (versionCode 16) — 19/10/2026

- Corrigido build 15: `ModalBottomSheet` e `rememberModalBottomSheetState` exigem `@kotlin.OptIn(ExperimentalMaterial3Api::class)` nos componentes da edição principal e PiP.
- Captura explícita de `maxWidth` no `BoxWithConstraints` de `TimelineRow` para evitar erro Kotlin causado pelo `Row` aninhado.
- Metadados, CI e documentação sincronizados. Nenhuma funcionalidade de edição removida.
- Compilação Android completa permanece pendente.

## 0.1.14 (versionCode 15) — 19/10/2026
- Alinhamento unificado das faixas da timeline (rótulo de 60 dp, margem de 12 dp); blocos retangulares.
- Toque em clipe abre Bottom Sheet compacto: cortar, duplicar, mover, excluir com confirmação e volume individual; prévia passa a seguir volume individual.
- Legendas manuais e importação SRT: campo de texto, horários, cor, tamanho e posição; faixa própria e efeito temporal no exportador.
- Transições independentes por par adjacente: corte direto, fade preto, fade branco (0,25 a 1 s); sem falsa dissolvência ou alteração de duração.
- Áudio: faixas independentes, ajuste de volume e remoção, prévia; extração de áudio do projeto ou clipe em M4A ou MP4 contendo AAC, salvando no SAF, opção salvar e acrescentar à timeline.
- Projeto JSON schemaVersion 4 com migração de campos anteriores e testes de SRT, duração e fades.
- Validações locais de sintaxe e cálculos; build Android, exportação múltipla, mixagem, efeitos GPU e aparelho ainda pendentes.

## 0.1.13 (versionCode 14) — 18/10/2026
- Reprodução em loop automático até pausar: REPEAT_MODE_ONE no clipe de Cortar e unir e REPEAT_MODE_ALL na playlist do editor principal.
- Seletor compacto de velocidade em ambas as telas (0,25× a 2×), alterando somente a prévia; vídeo exportado preserva velocidade original.
- PiPs na prévia do editor recebem a mesma velocidade do vídeo principal, para limitar defasagem durante reprodução acelerada ou lenta.
- Mini tutorial de cinco orientações abaixo dos controles Cortar e unir, incluindo o uso das ferramentas e a diferença entre prévia e exportação.
- Testes novos do catálogo de velocidades, documentação e workflow atualizados. Build Android e teste físico pendentes.

## 0.1.12 (versionCode 13) — 18/10/2026
- Cortar e unir simplificado: miniaturas clicáveis diretamente abaixo da prévia, sem marcador duplo sobre as imagens; único controle fino de cursor separado.
- Campo Tempo move o cursor ao digitar números válidos, com a máscara `mm:ss.d` preservada; sem campo “Ir para”/botões adicionais.
- Início/fim editáveis diretamente, botões de marcação sempre clicáveis com explicação quando o fim não pode ser definido (mínimo 250 ms).
- Dividir, Desfazer, mover antes/depois e Excluir agrupados na mesma linha compacta; desfazer de uma operação também cobre divisão, reordenação e exclusão.
- Mantidos quatro PiPs e edição sem modificar os arquivos originais. Pendente build Gradle e teste físico.

## 0.1.11 (versionCode 12) — 18/10/2026
- Editor Cortar e unir com preview ExoPlayer do clipe, controles de reprodução/seek de 1 segundo e miniaturas amostradas em IO.
- Linha do tempo compacta com marcadores RangeSlider para início/fim e cursor independente; ações Marcar início/Marcar fim; edição em rascunho com Aplicar corte, Desfazer última aplicação, Dividir, Reordenar e Excluir.
- Entrada de horários numérica autoformatada `mm:ss.d` com controle de limite, compartilhada pela busca na prévia e editor PiP.
- Mantidos projeto não destrutivo, múltiplos PiPs e compatibilidade com projetos anteriores. Workflow e documentação sincronizados. Build Android/teste em aparelho pendentes.

## 0.1.10 (versionCode 11) — 18/10/2026
- Bottom Sheet sem alça superior, mantendo botão Fechar; menos margens e espaços na tela PiP e no painel inferior.
- Horários tocáveis: campo digitável de busca na prévia e editor principal; início/fim do PiP com valor digitável, validação e barra preservada.
- Novo PiP começa após o último término existente, sem usar o cursor do vídeo; quando o último já alcançou o fim do projeto, exige confirmação para adicionar em período coincidente.
- Conflitos ESPAÇO+TEMPO têm aviso e opções Reposicionar automaticamente, Manter sobreposição ou Cancelar. Permissões de sobreposição ficam registradas por par de IDs no JSON (schema 3), sem liberar conflitos com futuros PiPs.
- Mantidos quatro PiPs, controles separados de áudio e exportação Media3. Compilação Android e testes no aparelho continuam pendentes.

## 0.1.10 (versionCode 11) — 18/10/2026
- Reorganizadas as ferramentas Posição, Tamanho, Duração e Mídia em **uma única linha** com quatro botões quadrados compactos e ícones.
- Os ajustes agora abrem em **ModalBottomSheet** na borda inferior; aparecem sobre os controles, sem empurrar o vídeo. Altura limitada, rolagem interna, tema do app e fechamento por gesto, toque fora ou botão.
- Mantidos os controles completos (9 posições, tamanho 5–90%/manual, duração e mídia), seleção de até quatro PiPs, áudio independente, anticolisão e compatibilidade de projetos.
- Sincronizados Gradle, tela Sobre, workflow de APK, README, AGENTS, release e validações. Compilação Android e testes físicos pendentes.

## 0.1.8 (versionCode 9) — 18/10/2026
- PiPs múltiplos (até 4) em um projeto, com seleção e edição independentes.
- Interface quadrada expansível na própria tela (Posição/Tamanho/Duração/Mídia); removidos os pop-ups de configuração.
- Motor de colisões espacial e temporal com prioridade para o PiP editado e aviso quando não houver espaço livre.
- Linha do tempo dividida em uma faixa por PiP, posição de início e duração corretas.
- Prévia e composição Media3 das até quatro fontes visuais; áudio independente para cada vídeo PiP.
- Modelo JSON `overlays` e `schemaVersion=2`; leitura automática do campo `pip` dos projetos antigos.
- Documentação/workflow atualizados para 0.1.8 e versionCode 9; APK ainda depende de compilação CI.

## 0.1.7 (versionCode 8) — 18/10/2026
- Corrigida compilação Debug/Release relatada no workflow 7: `PrimaryTabRow` do Material 3 requer `@OptIn(ExperimentalMaterial3Api::class)` na `DeviceMediaBrowser`.
- Preservadas biblioteca de mídia PiP, controles de posição/tamanho/duração e áudio individual do PiP.
- Versionamento e publicação APK sincronizados (Gradle, tela Sobre, workflow, README, release, validador).
- Ainda requer validação de build Android completo no GitHub Actions e testes físicos.

## 0.1.6 (versionCode 7) — 18/10/2026
- Menu compacto para PiP: janelas de Posição (9 atalhos), Tamanho (5% a 90%, slider e entrada manual) e Duração (início/fim e faixa).
- Nova biblioteca local de mídia: abas Imagens/Vídeos, filtros por pasta, miniaturas em grade com espaço reduzido, ícone e tempo dos vídeos. Usa MediaStore, permissões Android e alternativa pelo seletor de arquivos.
- Áudio de vídeo PiP em cartão próprio: silenciar/ativar sem perder volume escolhido; o estado é persistido e respeitado na prévia e na exportação.
- Ajustes de geometria para 5%, testes para entrada manual e serialização do estado mudo, documentação e workflow sequenciais.
- Compilação Android e verificação em aparelho pendentes.

## 0.1.5 (versionCode 6) — 18/10/2026
- PiP com visualização do vídeo principal, posição rápida 3×3, exibição proporcional e tempos de início/fim.
- Corrigida a posição central e limites da área de arrasto, guias centrais e prévia reproduzível.
- Adicionados testes Kotlin da geometria e da janela temporal.
- Workflow e documentação atualizados para UaiCortes-v0.1.5.apk.

# Histórico de versões

## 0.1.3 (versionCode 4)
- Opt-in pontual do Media3 UnstableApi no indicador de buffering da prévia, corrigindo falha Lint #3; sem ocultar erros do Lint.

## 0.1.2 (versionCode 3)
- Correção dos imports Media3 no motor de exportação e validador estático.

## 0.1.1 (versionCode 2)
- Atualização do setup Android para remover pacote SDK legado.

## 0.1.0 (versionCode 1)
- Base nativa do Uai Cortes e telas iniciais do editor.
