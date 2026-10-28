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
- Correção do build 26: `PathJoin` (símbolo inexistente no Compose usado pelo projeto) substituído por `StrokeJoin.Round` no contorno das legendas, mantendo a mesma intenção visual.
- Preservados os controles de estilo, sombra, entrelinhas e alinhamento da v0.1.29.
- Cinco variantes Release (arm32, arm64, x86, x86_64 e universal) permanecem configuradas para GitHub Releases.
- Compilação Kotlin, lint, empacotamento e publicação da v0.1.30 ainda dependem de nova execução do workflow.

## v0.1.29 (versionCode 30) · 19/10/2026
- Novos controles de sombra deslocada (0–6 dp, preta/branca), espaçamento entre linhas (100–160%) e alinhamento à esquerda, centro ou direita.
- Legendas antigas mantêm sombra desligada, espaçamento padrão e alinhamento centralizado.
- Prévia e Media3 recebem os novos efeitos, com necessidade de teste real em Android e comparação com vídeo exportado para comprovar fidelidade.
- Cinco APKs Release por ABI + universal continuam configurados para GitHub Releases; execução de CI pendente.

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

## Correção v0.1.22 (versionCode 23)
- Corrige o erro `No value passed for parameter onClip` em `MainActivity.kt:825` encontrado no build 18.
- Compilação Android e teste físico ainda pendentes.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

# Uai Cortes v0.1.22 · versionCode 23

## Correção do build 16
- Resolve uso de API `UnstableApi` do Media3 dentro das lambdas `AndroidView` que fazia `:app:lintDebug` falhar. Configurações de vídeo centralizadas em função com `@androidx.annotation.OptIn`.
- Mantém editor, quatro PiPs, legendas, cortes, áudio, fades e efeitos já implementados.

## Um Release com cinco APKs para baixar
- `UaiCortes-v0.1.22-arm32.apk` (ARM 32-bit, `armeabi-v7a`)
- `UaiCortes-v0.1.22-arm64.apk` (ARM 64-bit, `arm64-v8a`)
- `UaiCortes-v0.1.22-x86.apk` (Intel x86 32-bit)
- `UaiCortes-v0.1.22-x86_64.apk` (Intel x86 64-bit)
- `UaiCortes-v0.1.22-universal.apk` (universal, indicado quando não souber a ABI)

O CI publica **somente os cinco APKs Release** como arquivos diretos em GitHub Releases, não repete o pacote no Actions Artifacts e não reexecuta automaticamente por causa de tag criada pelo Release.
Todas as variantes usam o mesmo versionCode e a mesma assinatura do workflow. Utilize apenas uma por instalação.

## Limitações e validação
- Android Lint e compilação completa ainda precisam ser confirmados no próximo GitHub Actions.
- As variantes por ABI podem ser semelhantes em tamanho quando o aplicativo não embarca bibliotecas nativas específicas.
- Fades, exportação de múltiplos PiPs, SRT e extração AAC ainda precisam de validação em aparelhos reais. Dissolvência, MP3 e WAV não estão disponíveis.
