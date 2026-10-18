# Uai Cortes 0.1.5

Editor offline Android nativo em Kotlin, Jetpack Compose e Media3.

## Versões e ferramentas
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`.
- `versionName = 0.1.5`, `versionCode = 6`.
- Kotlin 2.1.21, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- Media3 1.10.1, ExoPlayer e Transformer para composição H.264/AAC.
- O editor não exige conta, internet nem acesso global ao armazenamento.

## Telas
- Início, Projetos, Editor, Cortar/Unir, PiP, Áudio, Transições (corte direto), Exportar MP4.
- Configurações, Geral, Tela e Sobre são **telas**, não pop-ups.
- Ícone azul integrado a `mipmap-*`; original também em `design/icone-uai-cortes.png`.

## Novidades na 0.1.5
- Na tela PiP, prévia com **vídeo principal real** e botão para reproduzir/pausar.
- 9 atalhos para posicionar o PiP nos cantos, centro e bordas, sem perder o arrasto livre.
- Ajuste por pinça, guias de centralização e limites que evitam posicionamento fora do quadro.
- Respeito à proporção original da imagem e ajuste `ContentScale.Fit` para logotipos.
- Controles claros **Começa em** e **Termina em**, com faixa visual da janela de exibição.
- Recalibração da escala do PiP na composição de exportação em função das dimensões dos clipes.
- Prévia com `TextureView` para sobrepor elementos Compose, que pode aumentar o consumo de energia.

## Compilação e APK
1. Envie os arquivos da pasta extraída para a **raiz** do repositório GitHub.
2. Abra **Actions > Gerar APK Uai Cortes > Run workflow** ou envie alterações para `main`.
3. O workflow valida código, roda testes, Android Lint e `assembleRelease` com API 36.
4. Ao concluir, o APK estará em **Releases > v0.1.5 > Assets**, com o nome **`UaiCortes-v0.1.5.apk`** (arquivo direto). Também aparece em Actions Artifacts, onde o GitHub compacta automaticamente o download.

### Assinatura
Secrets exigidos para assinatura estável: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. Não inclua keystore, senhas ou arquivo TXT de Secrets no repositório. Sem Secrets, o script aceita assinatura debug provisória e pode não atualizar versões anteriores.

## Validações e limitações
- Teste estático de versionamento, workflow e código fonte concluído.
- Lógica Kotlin isolada de coordenadas/intervalos compilada e testada.
- Ainda **não** foi realizado build Android Gradle nesta sessão nem teste em aparelho físico.
- A execução #3 (código 0.1.2) passou em Kotlin/assembleRelease, mas falhou no Lint; a 0.1.3 resolveu opt-in e a 0.1.4 adicionou upload obrigatório em Actions/Releases. Isso **não comprova** o build da 0.1.5.
- PiP e mixagem de áudio precisam ser avaliados em aparelhos reais; a prévia com dois ExoPlayers pode apresentar defasagem. A exportação e transparência de imagens PNG também exigem testes.
- Fade/dissolvência e corte preciso de áudio arbitrário continuam pendentes. A versão atual oferece **corte direto** entre clipes.
- Os projetos usam URI persistente de arquivos selecionados; ao mover ou apagar o arquivo original, ele pode ficar indisponível.

## Histórico
Ver `CHANGELOG.md`, `RELEASE_NOTES.md` e `docs/VALIDATION.md`.
