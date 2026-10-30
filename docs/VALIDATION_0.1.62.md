# Validação 0.1.62

- Testes JVM isolados de `ExportTaskCoordinator` e `ExportProgressUi`.
- Validação estrutural `python3 tools/validate_source.py`.
- Análise de sintaxe Kotlin e CRC do pacote ZIP.

## Ainda obrigatórios no GitHub Actions / Android
- `gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`.
- Iniciar exportação, rotacionar durante preparação e durante renderização e verificar continuidade sem duplicação.
- Cancelar durante processamento; confirmar descarte do temporário e preservação de fontes/projetos.
- Salvar na galeria, testar uso do Voltar, falha de armazenamento e confirmar MP4 reproduzível e completo.
- Encerrar o processo durante exportação; nenhuma retomada automática deve ser prometida.
