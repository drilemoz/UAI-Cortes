# Uai Cortes 0.1.77 · versionCode 78 — Interface Gemini

Data: 31/10/2026. Base real: fonte `UaiCortes-v0.1.76-Gemini-TTS.zip`.

## Alterações

- `HomeScreens.kt`: seção Gemini deixou de expandir o formulário nas Configurações; agora é `ItemCard` navegável como Geral, Tela e Diagnóstico.
- `MainActivity.kt`, `EditorNavigation.kt`: rota `gemini_settings`; Voltar retorna às Configurações ou à tela Áudio conforme a origem.
- `GeminiSettingsUi.kt`: tela interna mantém campo com visibilidade controlada, seis ações em grade 2×3, Testar/Salvar em linha própria, feedback em superfície compacta e avisos de privacidade expansíveis. Confirmações de exportar/copiar/remover/importar/testar e launchers SAF foram preservados.
- `GeminiNarrationUi.kt`: somente botão "Gerar por IA" fica na tela Áudio; `Dialog` de narração com rolagem interna e player/timeline depois de gerar; seletor modal com pesquisa, lista virtualizada e códigos `id` reais. Descrição livre em inglês deixou de ocupar a tela principal. Estado do texto é preservado ao fechar/abrir na mesma sessão; fechar fica indisponível enquanto gera.
- `GeminiVoiceSearch.kt`: filtro por nome/ID sem acentos e sem distinção de maiúsculas, sem modificar IDs oficiais.
- `GeminiVoiceSearchTest.kt`, `EditorNavigationTest.kt`: testes de filtro e navegação.
- Metadados sincronizados: Gradle, GitHub Actions, empacotador, validador, README, CHANGELOG, RELEASE_NOTES, AppReleaseNotes e AGENTS.

## Validação executada

- `python3 tools/validate_source.py`: aprovado, incluindo checagens de versão e interface v0.1.77.
- Parser Kotlin PSI em todas as fontes e testes: **166 `.kt`** analisados, **0 erros sintáticos**.
- Compilação Kotlin/JVM isolada de `GeminiVoiceSearch.kt` + `EditorNavigation.kt`: **8 verificações** aprovadas (nome, código, pesquisa vazia/inexistente, rotas).
- Integridade ZIP, parsers XML/JSON, verificação de arquivos e versão: executar no pacote final.

## Ainda pendente

- **Nenhuma compilação Android** (`assembleRelease`, `testDebugUnitTest`, `lintDebug`) executada: Gradle, wrapper JAR e SDK Android não estão disponíveis neste ambiente. Parser PSI não verifica resolução de tipos de Jetpack Compose.
- **Nenhum teste com chave Gemini real**, não houve gasto de cota nem verificação de conta/vozes nesta rodada.
- Pop-up, teclado virtual, sobreposição dos diálogos, rotação, legibilidade, acessibilidade, áudio Media3 e exportação MP4 precisam ser conferidos no APK físico. Não declarar essas etapas validadas.
- Nenhum APK foi compilado ou entregue; o ZIP é código-fonte. A publicação de cinco APKs Release segue o fluxo GitHub Actions.

## Compatibilidade e segurança

- Sem migração de projeto; `schemaVersion=4` preservado.
- Sem alterações no cliente HTTP Gemini, `GeminiAudioFiles`, `GeminiCredentialStore`, `ExportEngine`, Media3, áudio de clipe, PiP, legendas ou credenciais.
- Nenhuma chave criada, incluída no projeto ou solicitada. O catálogo efetivo depende da API, e o botão de prévia avisa sobre possível consumo de cota.
