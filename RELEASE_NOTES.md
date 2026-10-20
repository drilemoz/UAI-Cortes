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
