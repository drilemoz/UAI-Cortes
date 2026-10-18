# Uai Cortes 0.1.5 | Validação

## Alterações
- Tela PiP carrega ExoPlayer independente para mostrar o vídeo principal, usando clips do projeto.
- A proporção visual dos overlays vem das dimensões reais (foto por BitmapFactory.Options, vídeo por MediaMetadataRetriever).
- A imagem sobreposta usa ContentScale.Fit, proporção preservada, e decodificação por amostragem para economizar RAM.
- Nove atalhos de posição e gestos usam matemática normalizada e limites para manter o conteúdo inteiro no quadro.
- Sliders mostram Começa em / Termina em, janela de exibição e limites mínimos de duração.
- Compositor de exportação recalcula escala em função da resolução das fontes, buscando manter correspondência com a prévia.
- O editor principal também usa área de vídeo proporcional, evitando posicionamento sobre barras pretas.
- TextureView apenas nas prévias editáveis para composição previsível com Compose; pode consumir mais energia que SurfaceView, avaliar em aparelhos fracos.

## Verificações locais
- Validação estática do workflow (API 36, versionCode 6, Artifacts e Release Assets).
- Compilação isolada com kotlinc do cálculo geométrico puro.
- Testes de limites/atalhos e tempo em Kotlin isolado.
- Estrutura, YAML e CRC do ZIP verificáveis.

## Pendências
- Compilação Gradle Android completa em ambiente com SDK 36.
- Android Lint completo e testes de interface no aparelho.
- Exportação de PiP imagem/vídeo, sincronismo entre codecs, transparência de PNG e proporções em vídeos verticais.
- Transições fade/dissolvência (não implementadas).
