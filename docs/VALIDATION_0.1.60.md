# Uai Cortes 0.1.60: verificações e limites

## Alterações
- Coordenador independente `MediaTaskCoordinator` com `MediaTaskGate` baseado em tickets únicos.
- Botão **Cancelar importação** no diálogo de metadados de vídeo/áudio/PiP.
- Cancelamento quando a composição termina ou muda projeto, rota ou ação.
- Regras de seleção, JSON (schemaVersion=4), exportação Media3 e qualidade da 0.1.56 preservadas.

## Validado neste ambiente
- Compilação isolada Kotlin/JVM de `MediaImportTasks`, `MediaTaskCoordinator` e testes, com stubs somente para classes Android/auxiliares.
- Cinco métodos de teste executados e aprovados para gate, destino, cancelamento, erro e nova importação.
- Verificação estrutural `python3 tools/validate_source.py` e consistência de versões.
- Integridade ZIP e parsing sintático Kotlin (quando disponível).

## Pendente
- Build completo Gradle `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no GitHub Actions.
- Testes físicos: cancelar importações SAF durante rotação, navegação e troca de projeto; testar vídeo longo/URI sem permissão.
- Verificar MP4 real, bitrate e reprodução nas opções 720p/1080p.

**Limite:** cancelamento de operações bloqueantes do `ContentResolver` pode ser cooperativo, não interrupção imediata de I/O nativo. Não há retomada de importação após morte do processo.
