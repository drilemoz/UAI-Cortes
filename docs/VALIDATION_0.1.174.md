# Uai Cortes 0.1.174 — Relatório de implementação e validação

**Base:** código-fonte 0.1.173. **Nova versão:** 0.1.174, versionCode 175. **Data:** 05/11/2026. **Status:** fonte preparada para validação Android, sem APK compilado nesta sessão.

## Funcionalidades implementadas

1. **Edição por gesto na timeline principal:** um toque seleciona e exibe alças; arrastar o centro move a faixa independente ou reordena clipes principais; arrastar as bordas apara. O gesto aplica uma alteração somente quando o dedo é solto. Reproduzir pausa enquanto o usuário edita.
2. **Faixas:** Vídeo e Imagem continuam clipes sequenciais (sem lacunas); PiP, Áudio, Vozes não vinculadas e Legendas trabalham com posições temporais absolutas. Áudio/voz vinculados permanecem presos ao clipe original, com edição pelo menu específico.
3. **Limites:** corte do vídeo respeita a fonte e mínimo de 250ms; áudio respeita a duração da fonte; imagem principal redimensionada diretamente aceita 1s a 15min; PiP não pode exceder o projeto ou sua mídia e bloqueia novas colisões não autorizadas.
4. **Ponto de entrada de vídeo PiP:** novo campo opcional `Overlay.sourceStartMs` (default 0) em `schemaVersion=4`; aplicado à prévia `PipLayer`, às trilhas de áudio/vídeo do `ExportEngine`, aos limites do editor PiP e reiniciado ao substituir a mídia.
5. **Sincronismo:** elementos inteiramente contidos num clipe seguem o clipe quando reordenado; elementos que atravessam fronteiras conservam seus horários com aviso. Aparar uma entrada preserva a posição temporal das legendas/PiPs/áudios totalmente contidos na parte mantida; se houver elementos contidos no trecho descartado, a operação é bloqueada com explicação para não perder conteúdo silenciosamente.
6. **Usabilidade:** zoom horizontal com botões 1×, 2×, 4× e 8×; área auxiliar após o último clipe para estender fotos finais; encaixe magnético opcional de até 350ms; Desfazer/Refazer de até 15 operações gestuais em memória; ferramentas avançadas permanecem acessíveis pelo botão Editar.
7. **Compatibilidade:** projetos existentes mantêm os mesmos IDs, arquivos, formato JSON e preferências; nova propriedade de PiP é opcional; preservadas a galeria integrada, a correção da importação de imagens 0.1.173, diagnósticos e APIs.

## Arquivos de implementação

- `app/src/main/java/br/com/uaicortes/TimelineDirectEdit.kt` **novo** — políticas de edição, limites, aviso de conflitos e encaixe.
- `UnifiedEditorTimeline.kt` — nova seleção/gestos, alças, scroll/zoom, histórico e alternância de encaixe.
- `EditorScreen.kt` — integração com o projeto, pausa durante gestos, leitura assíncrona de duração da fonte, commits únicos e undo/redo.
- `ProjectStore.kt` — offset opcional e compatível para PiP.
- `EditorTimelineUi.kt` — prévia PiP com offset correto da origem.
- `ExportEngine.kt` — offset no recorte Media3 do vídeo e áudio PiP.
- `PipScreen.kt` — limites de duração descontando a entrada da mídia.
- `OverlayOperations.kt` — reinício do offset ao substituir mídia PiP.
- `AppReleaseNotes.kt`, `AGENTS.md`, `VERSION`, Gradle, workflow, `tools/package_apks.py`, JSON de identidade, README, CHANGELOG e RELEASE_NOTES — versionamento e documentação.
- `app/src/test/java/br/com/uaicortes/TimelineDirectEditTest.kt` **novo** e `OverlaySourceStartPersistenceTest.kt` **novo** — testes JVM para o CI.
- `tools/test_timeline_direct_174_contract.py` **novo**, `tools/test_ci172_unified_timeline_and_api.py`, `tools/test_voice_ia_contract.py` e `tools/validate_source.py` — contrato estrutural novo e adequação aos componentes substituídos, sem desativar verificações do motor antigo.

## Testes executados nesta sessão

| Verificação | Resultado |
|---|---|
| Scripts Python de contrato `tools/test_*.py` | **70/70 aprovados** |
| `tools/validate_source.py` | **Aprovado** |
| `tools/check_version_sync.py` | **Aprovado** — 0.1.174 / 175 |
| Parser Kotlin PSI | **10 arquivos, zero erros sintáticos** |
| Compilação JVM isolada com stubs do motor `TimelineDirectEdit` | **17 verificações Kotlin aprovadas** — limites de vídeo/foto/PiP/áudio, reordenação, legenda, bloqueio ao descartar conteúdo e encaixe magnético |
| ZIP fonte, CRC, reextração e validação da fonte extraída | Executado no empacotamento da entrega |

**Não executados:** compilação Gradle/Android completa, Android Lint, JUnit Gradle (incluindo novos testes), testes instrumentados Android, prévia real, gestos em touchscreen, importação real, exportação MP4 e reprodução de PiP com offset. O ambiente não tem Gradle instalado e o script `gradlew` não contém um wrapper JAR executável; `bash gradlew --version` retorna orientação para usar Gradle 8.11.1 ou GitHub Actions. Análise PSI não equivale a build.

## Limitações e próximos testes

- O zoom por enquanto é por botões; **pinça de dois dedos não foi adicionada**. As faixas principais são sequenciais e mover equivale a reordenar, não criar lacunas.
- Ao reordenar clipes, elementos que cruzam fronteiras mantêm o horário e exigem revisão manual. Overlays com nova colisão sem autorização não são movidos automaticamente.
- O histórico de gestos está em memória da tela; ao sair e voltar, as alterações permanecem salvas no projeto, mas a pilha Desfazer/Refazer não persiste.
- A recuperação de URIs perdidas de vídeos legados e a correção definitiva de instalação Vosk permanecem pendentes de desenvolvimento específico; não foram alteradas nesta versão.
- Testar no CI as cinco ABIs Release, Lint/Media3, testes JUnit e instrumentados; no Android, cortar vídeo de 10s e tentar ultrapassar o limite, ampliar foto, aparar PiP pela esquerda, arrastar áudio, voz e legenda, reabrir projetos antigos e comparar áudio/vídeo do MP4 exportado com a prévia.
- **Não executar o teste físico sobre o único backup de um projeto importante**; salve uma cópia antes.
