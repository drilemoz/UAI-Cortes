# Uai Cortes 0.1.161 — implementação e validação do Estúdio de Cenas

Data: 05/11/2026. Base recebida: `UaiCortes-v0.1.160-Gemini-Consumo-Compacto-Tarifas.zip`.

## Entrega implementada no código

- `FlowPromptScreen` agora abre **Estúdio de Cenas** com História, Cenas e Exportar para Flow. O editor antigo foi preservado integralmente como `FlowLegacyPromptScreen` em Avançado, inclusive Texto → vídeo, Vídeo de referência, estilo-base, restauração 3D, Gemini por cena, Batch, salvamento e cópia.
- História: ideia, sugestões locais Surpreenda-me, roteiro colado, durações de 15 a 180 s ou personalizada (5–900), estilo/gênero, campos editáveis, Gemini JSON para título, sinopse, personagens, cenários, começo, desenvolvimento, reviravolta e final. Confirmação de envio e revisão da proposta antes de aplicá-la. Aprovação obrigatória para divisão com IA.
- Cenas: divisão por Gemini em JSON, aprovação antes de substituir; cenas individuais com título, visual, personagens, ações, falas pt-BR, sons, câmera, estados inicial/final e prompt; edição, duplicação, exclusão confirmada, reordenação, revisão de regeneração individual, cópia, geração de imagem e importação de quadro pelo SAF.
- Referências: fichas editáveis/persistidas com aparência, roupas, voz, personalidade e objetos de cada personagem; ficha de local e objetos; contexto textual anterior e imagem de cena anterior são utilizados na geração quando disponíveis. O modelo pode não manter continuidade; o usuário precisa revisar imagens reais do Flow.
- Imagens: consulta manual de modelos `generateContent` de imagem listados pela API para a chave protegida; seleção explícita; referência oficial de custo em USD por imagem de saída 1K quando verificável, com aviso de custo parcial; confirmação antes de gerar; pedido de imagem 9:16 com `responseModalities` e `imageConfig`; processamento de resposta inline PNG/JPEG/WebP, limites de tamanho/dimensões, salvamento privado, prévia, regeneração, substituição, importação, exportação SAF e compartilhamento Android via FileProvider.
- Exportar para Flow: cartões de status Não preparada/Pronta/Vídeo importado, miniaturas, prompts copiáveis, exportação de imagem e associação de vídeo do Flow por seletor de documentos; ZIP via Storage Access Framework contendo roteiro completo, fichas de personagens e cenários, prompts UTF-8 por cena, roteiro de cada cena, imagens e JSON de ordem/continuidade. URIs locais dos vídeos não são incluídas no ZIP, que não contém chave Gemini.
- Persistência privada `FlowStudioStorage` por projeto, usando `AtomicFile` para proteger rascunhos. Preserva o JSON legado `flowPromptDraft`, catálogo Flow existente, formato `schemaVersion=4` e editores de vídeo. Sem transmissão automática e sem API Veo.

## Arquivos novos

- `app/src/main/java/br/com/uaicortes/FlowStudioModel.kt`
- `app/src/main/java/br/com/uaicortes/FlowStudioAi.kt`
- `app/src/main/java/br/com/uaicortes/FlowStudioScreen.kt`
- `app/src/main/java/br/com/uaicortes/FlowStudioStorage.kt`
- `app/src/main/java/br/com/uaicortes/GeminiSceneImageClient.kt`
- `app/src/test/java/br/com/uaicortes/FlowStudioModelTest.kt`
- `app/src/androidTest/java/br/com/uaicortes/FlowStudioStorageAndroidTest.kt`
- `tools/test_flow_studio_161_contract.py`
- `tools/StudioKotlinSyntaxCheck.kt`
- `docs/VALIDATION_0.1.161.md`

## Arquivos alterados

`AGENTS.md`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`, `VERSION`, `app/build.gradle.kts`, `app_identity.json`, `github-manager.json`, `.github/workflows/android-apk.yml`, `tools/package_apks.py`, `app/src/main/java/br/com/uaicortes/AppReleaseNotes.kt`, `FlowPromptScreen.kt`, `FlowWorkspaceScreen.kt`, `GeminiVideoCaptionClient.kt`, `HomeScreens.kt`, `app/src/main/res/xml/diagnostic_file_paths.xml`, `app/src/androidTest/java/br/com/uaicortes/HomeAboutPreviewAndroidTest.kt`, `tools/test_flow_watermark_contract.py`, `tools/test_home_three_cards_contract.py`.

## Verificações executadas neste ambiente

- Extração e inspeção do ZIP recebido, incluindo `AGENTS.md` e arquitetura do Flow/Gemini.
- `tools/check_version_sync.py`: **aprovado** (`0.1.161`, versionCode `162`).
- `tools/test_version_sync.py`: **aprovado** (7 divergências simuladas detectadas).
- `tools/validate_source.py`: **aprovado** (regras estruturais legadas).
- `tools/test_flow_watermark_contract.py`: **aprovado**.
- `tools/test_gemini_flow_batch_159_contract.py`: **aprovado**.
- `tools/test_home_three_cards_contract.py`: **aprovado**.
- `tools/test_flow_studio_161_contract.py`: **aprovado**.
- Parser PSI da distribuição Kotlin: **10 fontes Kotlin novas/alteradas sem erros sintáticos**. Não verifica tipos, recursos Android nem contratos Compose.
- Empacotamento por `tools/package_source.py`: arquivo completo verificado por reextração e `validate_source.py` (após a execução de geração do ZIP).

## NÃO validado neste ambiente

- **Build Android real, testes JUnit, testes instrumentados, Compose e APK Release**. Gradle 8.11.1 e SDK Android 36 não estão instalados no ambiente. O launcher `gradlew` enviado é um script que requer Gradle instalado; `bash gradlew --offline :app:compileDebugKotlin` informou explicitamente ausência de Gradle. Não alegar que o aplicativo compila ou que existem cinco APKs gerados.
- **Chamadas reais de texto/imagem Gemini**: nenhuma chave/API foi fornecida nem houve consentimento para gastos. O código usa o endpoint oficial, mas acesso ao modelo, resposta JSON e cobrança precisam ser verificados com uma chave de projeto autorizada. `models.list` não garante que uma geração será aceita.
- **SAF em aparelho**: testar arquivos grandes, nomes acentuados, permissões persistentes, fechamento/reabertura, comportamento do provedor, compartilhamento para Flow e ZIP em Android 16.
- **Teste visual físico**: temas claro/escuro, fontes grandes, 720p/1080p, rotação, acessibilidade, 15s a 3min, geração sem internet, falhas/quota 429, imagens já existentes e alterações simultâneas.
- **Importar vídeo não efetua montagem automática**: associa o URI do vídeo gerado a uma cena; edição e montagem permanecem no editor de vídeo existente.

## Dependências e limites conhecidos

- História/cenas usam o modelo de **texto** selecionado nas Configurações Gemini. Imagem exige chave que retorne modelos `*-image` via `models.list`, e chamadas confirmadas pelo usuário; alguns exigem faturamento habilitado.
- Referências visuais só são enviadas quando existentes e o usuário confirma geração. Até duas imagens são comprimidas para JPEG antes do envio.
- O custo exibido para imagem é referência **parcial** da saída 1K, datada, nunca previsão de conta completa ou créditos restantes. Documentação oficial: https://ai.google.dev/gemini-api/docs/image-generation e https://ai.google.dev/gemini-api/docs/pricing .
- A geração de divisão em muitas cenas pode falhar por limite de tokens do modelo; a resposta deve ser revista e confirmada. Nenhuma chamada é repetida automaticamente após falha ambígua.
- Sem API pública de projeto editável no Google Flow; prompts e imagens são transferidos manualmente. Sem promessa de identidade contínua perfeita.
