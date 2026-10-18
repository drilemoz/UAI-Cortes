# Uai Cortes 0.1.6

Editor offline Android nativo em Kotlin, Jetpack Compose e Media3.

## Versões e ferramentas
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`.
- `versionName = 0.1.6`, `versionCode = 7`.
- Kotlin 2.1.21, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- Media3 1.10.1, ExoPlayer e Transformer para composição H.264/AAC.
- O editor não exige conta nem internet. A biblioteca PiP solicita acesso às fotos/vídeos do Android, integral ou parcial conforme consentimento; o seletor de documentos continua como alternativa.

## Telas
- Início, Projetos, Editor, Cortar/Unir, PiP, Áudio, Transições (corte direto), Exportar MP4.
- Configurações, Geral, Tela e Sobre são **telas**, não pop-ups.
- Ícone azul integrado a `mipmap-*`; original também em `design/icone-uai-cortes.png`.

## Novidades na 0.1.6
- Tela PiP resumida em três ações com ícones e janelas temáticas: **Posição**, **Tamanho** e **Duração**.
- Posição: nove atalhos com ícones; arrastar a prévia continua funcionando.
- Tamanho: 5, 10, 15, 20, 25, 30, 40, 50, 60, 75, 90%, ajuste por barra, pinça e campo manual de 5 a 90%. Mantém o aspecto e respeita a área do vídeo.
- Duração: controles de início e fim, indicação da faixa ativa e limites de intervalo.
- Biblioteca PiP própria, com abas Imagens/Vídeos, filtros por pasta do MediaStore (DCIM, Pictures, Movies, Download e outras indexadas), miniaturas com espaçamento de 4 dp e vídeo identificado por duração.
- Permissões conforme a versão do Android, inclusive acesso parcial a mídias selecionadas no Android 14+; alternativa para escolher arquivo usando o gerenciador do sistema.
- Cartão separado de som do PiP em vídeo: liga/desliga e volume; o volume é preservado ao silenciar. Persistência em projetos e aplicação na prévia/exportação.
- Editor e composição PiP seguem offline, sem IA, autenticação ou serviços remotos.

## Compilação e APK
1. Envie os arquivos da pasta extraída para a **raiz** do repositório GitHub.
2. Abra **Actions > Gerar APK Uai Cortes > Run workflow** ou envie alterações para `main`.
3. O workflow valida código, roda testes, Android Lint e `assembleRelease` com API 36.
4. Ao concluir, o APK estará em **Releases > v0.1.6 > Assets**, com o nome **`UaiCortes-v0.1.6.apk`** (arquivo direto). Também aparece em Actions Artifacts, onde o GitHub compacta automaticamente o download.

### Assinatura
Secrets exigidos para assinatura estável: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. Não inclua keystore, senhas ou arquivo TXT de Secrets no repositório. Sem Secrets, o script aceita assinatura debug provisória e pode não atualizar versões anteriores.

## Validações e limitações
- Teste estático de versionamento, workflow e código fonte concluído.
- Lógica Kotlin isolada de coordenadas/intervalos compilada e testada.
- Ainda **não** foi realizado build Android Gradle nesta sessão nem teste em aparelho físico.
- A execução #3 (código 0.1.2) passou em Kotlin/assembleRelease, mas falhou no Lint; a 0.1.3 resolveu opt-in e a 0.1.4 adicionou upload obrigatório em Actions/Releases. Isso **não comprova** o build da 0.1.6.
- PiP e mixagem de áudio precisam ser avaliados em aparelhos reais; a prévia com dois ExoPlayers pode apresentar defasagem. A exportação e transparência de imagens PNG também exigem testes.
- Fade/dissolvência e corte preciso de áudio arbitrário continuam pendentes. A versão atual oferece **corte direto** entre clipes.
- Arquivos escolhidos no seletor do sistema usam URI persistente. Itens escolhidos pela biblioteca MediaStore dependem da permissão de leitura ativa e da existência do arquivo original; retirar a permissão pode tornar o PiP indisponível.

## Histórico
Ver `CHANGELOG.md`, `RELEASE_NOTES.md` e `docs/VALIDATION.md`.
