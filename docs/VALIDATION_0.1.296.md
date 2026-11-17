# Validação da 0.1.296

- Base 0.1.295, versionCode 297. Schema JSON 4, assinatura e variantes de APK preservados.
- Os módulos Estúdio de Cenas e Animar Imagens não têm código, telas ou rotas ativos. Nenhum arquivo ou preferência antiga é apagado automaticamente.
- Metadados de cena dentro de um projeto do editor são transportados como JSON opaco. A compatibilidade não recria a tela retirada. Clipes e MP4s já gerados continuam disponíveis como mídia normal.
- Os ensaios de navegação cobrem rotas removidas e recuperação da galeria normal. `LegacySceneProjectWatermarkTest` cobre o ciclo de leitura e gravação de um projeto com metadados antigos. `HomeAboutPreviewAndroidTest` cobre a ausência dos dois atalhos e as seis ações restantes.
- No aparelho, abrir um projeto antigo, importar uma foto e um vídeo, aplicar uma legenda e exportar. Conferir as ferramentas de voz e abrir um MP4 criado antes da atualização.
- A validação estática não substitui Kotlin/Gradle, Lint nem testes no aparelho.
