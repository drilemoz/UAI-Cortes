# Validação Uai Cortes 0.1.79 — versionCode 80

## Escopo

- Subtítulos: `y=.92` por padrão de novos cues, sem alterar coordenadas explícitas legadas; controle vertical e atalho Rodapé 8% em Estilo/Editar todas. Em lote, a opção de copiar posição continua exigindo marcação explícita.
- Texto adaptativo: cálculo proporcional compartilhado `SubtitleAutoFit` a partir das larguras tipográficas e da largura disponível, aplicado no Compose e no `TimedSubtitleOverlay` Media3; busca limitar aproximadamente a três linhas sem `ellipsis`, preservando o texto original. Em textos muito grandes, pode ultrapassar três linhas para evitar ocultação.
- PiP: propriedade opcional `opacity` 0–1, default 1; JSON no schemaVersion 4, slider na tela PiP, alpha de imagem/Textura na prévia e `setAlphaScale` no `PipCompositor` do exportador. Áudio separado do PiP não é silenciado pela transparência.
- Captura de quadro: `player.pause()` antes de ler a posição atual real, evitar `currentMs` defasado, mostrar feedback enquanto a extração PNG roda em IO e manter a prévia pausada. Somente quadro original (sem legendas/PiP).
- Timeline: rótulos compactos `Clipe N`, mantendo título real em metadados e nos detalhes.

## Verificações executadas

1. `python3 tools/validate_source.py` — **aprovado** após alterações de versionamento e assertivas novas.
2. Kotlin PSI — **173 arquivos Kotlin, 0 erros sintáticos** (parser, não checagem de símbolos ou tipos).
3. Parser XML ElementTree — **6 XMLs válidos**.
4. Kotlin JVM isolado: **5 verificações** de `SubtitleAutoFit` e **3 verificações** de `SubtitlePlacement`, todas aprovadas. Os testes JUnit adicionados são para execução posterior no Gradle.
5. CRC/ZIP integral e hashes — conferir ao empacotar.

## Pendências e limites

- **Gradle Android não executado**: o ambiente não dispõe de Gradle 8.11.1 e o wrapper neste projeto não contém o JAR necessário. Não há APK compilado nem Release nesta entrega.
- Os testes `PipOpacityPersistenceTest` (JSONObject) e `SubtitleAutoFitTest` precisam rodar no workflow `:app:testDebugUnitTest`; não foram executados por Gradle neste ambiente.
- Testar em aparelho: alpha de imagem e vídeo com TextureView, alfa real no MP4 exportado pelo compositor, back/rotação, fontes e legendas extensas no Preview e no MP4, posições de projetos antigos, resposta do botão Capturar e latência do `MediaMetadataRetriever.OPTION_CLOSEST`.
- O controle de opacidade não altera o volume do áudio contido no PiP; para silenciar, usar o controle de áudio separado.
- A captura produz PNG do vídeo original. Um PNG com PiP e legendas embutidos exigiria um pipeline adicional de captura de composição e não foi implementado.
- Arquivos existentes de usuários não são modificados automaticamente; a nova posição de rodapé é padrão para legendas novas, ou pode ser selecionada em `Editar todas`.

## Compatibilidade

`schemaVersion=4`, Vosk offline, Gemini TTS, assinatura Android, configuração API 36, bibliotecas Media3 e cinco variantes de APK em GitHub Actions permanecem intactos. Próxima versão: 0.1.80, versionCode 81.
