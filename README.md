# Uai Cortes 0.1.8

Editor offline Android nativo em Kotlin, Jetpack Compose e Media3.

## Versões e ferramentas
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`.
- `versionName = 0.1.8`, `versionCode = 9`.
- Kotlin 2.1.21, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- Media3 1.10.1, ExoPlayer e Transformer para composição H.264/AAC.
- O editor não exige conta nem internet. A biblioteca PiP solicita acesso às fotos/vídeos do Android, integral ou parcial conforme consentimento; o seletor de documentos continua como alternativa.

## Telas
- Início, Projetos, Editor, Cortar/Unir, PiP, Áudio, Transições (corte direto), Exportar MP4.
- Configurações, Geral, Tela e Sobre são **telas**, não pop-ups.
- Ícone azul integrado a `mipmap-*`; original também em `design/icone-uai-cortes.png`.

## Novidades na 0.1.8
- Até **4 PiPs por projeto**, com vídeos ou imagens independentes, seleção de camada e opção de adicionar, substituir ou remover.
- Posição, escala 5–90%, duração, volume e silêncio independentes. Áudio desligado em um PiP não silencia os outros.
- Quatro botões quadrados Posição/Tamanho/Duração/Mídia. Ao tocar, os botões dão lugar a um painel expansível, **dentro da tela**, com animação vertical. Nenhuma janela modal para esses controles.
- Prévia de até quatro PiPs ativos no instante do vídeo, edição por arrasto/pinça do selecionado, e faixas de tempo individuais no editor.
- Reposicionamento automático determinístico considerando retângulos, margens e **interseção temporal**. A camada editada recebe prioridade; as demais mudam de posição apenas se necessário. Se não houver espaço, bloqueia a mudança com aviso.
- Projeto JSON com `schemaVersion=2`, campo `overlays[]`, IDs persistentes e **migração de `pip` legado** sem descartar projetos antigos.
- `ExportEngine`: compõe as até quatro sequências visuais e trilhas de áudio independentes. **Compatibilidade real com 4 decodificadores, sincronismo e transparência depende de build e teste em aparelho**.
- Biblioteca de mídia MediaStore por tipo/pasta, alternativa SAF, e demais funcionalidades da 0.1.7 preservadas.

## Compilação e APK
1. Envie os arquivos da pasta extraída para a **raiz** do repositório GitHub.
2. Abra **Actions > Gerar APK Uai Cortes > Run workflow** ou envie alterações para `main`.
3. O workflow valida código, roda testes, Android Lint e `assembleRelease` com API 36.
4. Ao concluir, o APK estará em **Releases > v0.1.8 > Assets**, com o nome **`UaiCortes-v0.1.8.apk`** (arquivo direto). Também aparece em Actions Artifacts, onde o GitHub compacta automaticamente o download.

### Assinatura
Secrets exigidos para assinatura estável: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. Não inclua keystore, senhas ou arquivo TXT de Secrets no repositório. Sem Secrets, o script aceita assinatura debug provisória e pode não atualizar versões anteriores.

## Validações e limitações
- Validação estática e smoke test Kotlin de colisões concluídos.
- Motor Kotlin isolado de posicionamento/intervalos compilado e testado.
- Ainda **não** foi realizado build Android Gradle nesta sessão nem teste em aparelho físico.
- A execução #3 (código 0.1.2) passou em Kotlin/assembleRelease, mas falhou no Lint; a 0.1.3 resolveu opt-in e a 0.1.4 adicionou upload obrigatório em Actions/Releases. O build 7 (0.1.6) falhou especificamente em `MediaBrowser.kt:165` por falta de opt-in Material 3; a compilação da 0.1.8 ainda não foi confirmada.
- A prévia de até quatro ExoPlayers de PiP pode apresentar limitação de decodificadores/defasagem em aparelhos reais. A exportação e transparência de imagens PNG também exigem testes.
- Fade/dissolvência e corte preciso de áudio arbitrário continuam pendentes. A versão atual oferece **corte direto** entre clipes.
- Arquivos escolhidos no seletor do sistema usam URI persistente. Itens escolhidos pela biblioteca MediaStore dependem da permissão de leitura ativa e da existência do arquivo original; retirar a permissão pode tornar o PiP indisponível.

## Histórico
Ver `CHANGELOG.md`, `RELEASE_NOTES.md` e `docs/VALIDATION.md`.
