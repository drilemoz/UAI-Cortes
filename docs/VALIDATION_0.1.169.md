# Uai Cortes 0.1.169 — Implementação de imagens como clipes

**Data:** 05/11/2026 · **versionName:** 0.1.169 · **versionCode:** 170 · **VideoProject.schemaVersion:** 4 (sem migração obrigatória).

## Objetivo

Permitir adicionar JPG/PNG/WebP/HEIC/HEIF/BMP da galeria ao editor de vídeo como um clipe visual com duração configurável, em vez de somente uma sobreposição PiP; caso comum: capa parada antes do primeiro vídeo.

## Funcionalidades e funcionamento

- Nova ação **Imagem** no carrossel inferior do editor → seletor SAF `OpenDocument` → duração **1–30 segundos** → posição **Capa no início / antes do selecionado / depois do selecionado / no final** → confirmação explícita.
- MIME validado, limite de 24 MiB e 24 megapixels; formato animado GIF recusado. Permissão de leitura de `content://` preservada por `takePersistableUriPermission` quando concedida pelo Android.
- Foto é um `Clip` com `isImage=true`, `imageMimeType` e duração persistida; os outros clipes permanecem. A miniatura e a faixa Vídeo a identificam como foto.
- Na tela de edição da foto: amostra reduzida carregada em IO, alterar duração, mover, duplicar e excluir pela timeline, sem excluir o arquivo original da galeria.
- Player Media3 trata imagens como `MediaItem.Builder.setImageDurationMs`; exportador Media3 Transformer cria os quadros estáticos com `EditedMediaItem.Builder.setFrameRate(30)` e mantém efeitos/textos/áudio de fundo. **Não há renderização de MP4 intermediário.**
- Extração e reconhecimento de fala ignoram a foto enquanto computam o deslocamento total dos outros clipes. A foto não tem áudio original.
- Ao inserir ou redimensionar foto, deslocam-se elementos com tempo absoluto posteriores: PiP, legenda (inclusive marcações de palavras) e áudio extra global. A faixa vinculada a `clipId` acompanha seu clipe por posição calculada; trilha de música do projeto continua do início. Transições não mais contíguas são removidas de forma conservadora.
- Projetos antigos sem `isImage` são carregados como vídeos, com schema 4 preservado. Módulos Gemini, Wizard Flow, Redes Sociais e cinco saídas de APK por arquitetura preservados.

## Arquivos adicionados

- `app/src/main/java/br/com/uaicortes/StaticImageClip.kt` — regras de foto, importação, tempo e MediaItem.
- `app/src/main/java/br/com/uaicortes/MainImageInsertionUi.kt` — seletor, duração, posição, confirmação, persistência.
- `app/src/test/java/br/com/uaicortes/ImageClipPolicyTest.kt` — JUnit de inserção, duração, JSON, sincronismo.
- `tools/test_main_image_clip_169_contract.py` — testes de contrato do novo fluxo.

## Arquivos de código alterados

- `ProjectStore.kt`, `MainActivity.kt`, `EditorScreen.kt`, `TrimScreen.kt`, `ExportEngine.kt`, `HomeScreens.kt`, `MediaImportTasks.kt`, `TransitionScreen.kt`.
- Prévias atualizadas: `ExportScreen.kt`, `AudioScreen.kt`, `PipScreen.kt`, `SubtitleScreen.kt`, `VoiceIaScreen.kt`, `TextWatermarkScreen.kt`, `AudioReferenceAlignmentUi.kt`, `GeminiNarrationComparisonUi.kt`.
- Proteções de áudio/transcrição: `AudioExtractionEngine.kt`, `AutoSubtitleEngine.kt`, `ReferenceAudioProbe.kt`.
- Versionamento: `AppReleaseNotes.kt`, `app/build.gradle.kts`, `.github/workflows/android-apk.yml`, `VERSION`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`, `AGENTS.md`, `app_identity.json`, `github-manager.json`, `tools/package_apks.py`.

## Testes efetivamente executados

| Verificação | Resultado |
| --- | --- |
| `python tools/validate_source.py` | Aprovado |
| `python tools/check_version_sync.py` | Aprovado: 0.1.169 + 170 |
| Todos os `tools/test_*.py` (65 scripts) | **65/65 aprovados** em ~18 s, paralelamente |
| Parser PSI do Kotlin, 23 fontes (incluindo os novos testes) | Zero erros sintáticos |
| `bash ./gradlew --version` | **Não executável**: Gradle não instalado neste ambiente |
| Testes Android JUnit/Instrumentados, APK CI | **Não executados**; dependem de Gradle/Android SDK ou workflow |
| MP4 real, prévia de imagem, rotação EXIF, MediaStore, leitura SAF | **Não testados em dispositivo** |

## Limitações e riscos

- O Android/Media3 podem interpretar orientação EXIF e fotos HEIC diferentemente em aparelhos específicos; validar a prévia e o MP4 no aparelho.
- Uma foto perdida, movida ou com acesso SAF revogado não poderá ser exportada; selecionar novamente no sistema.
- Elementos temporizados que atravessam o ponto de inserção podem precisar de ajuste manual; a regra automática só desloca os que começam depois do ponto.
- Música global continua tocando desde o começo, inclusive na capa, intencionalmente.
- Não há fade especial ou animação Ken Burns nesta opção; a imagem permanece estática. Para animar imagens, o app possui módulo separado.
- Não foi possível provar compilação, UI e exportação real Android neste ambiente. Não considerar APK aprovado antes de CI e teste físico.
