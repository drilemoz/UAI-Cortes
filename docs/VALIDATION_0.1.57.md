# Validação 0.1.57

Entrada: workflow `Gerar APK Uai Cortes 50`, 30/10/2026, 166 testes, 1 falha em `EditorWorkspaceStateTest.kt:62`.

## Checagens locais (executadas)

- `python3 tools/validate_source.py`: aprovado.
- Parser Kotlin PSI: 108 arquivos, sem erros sintáticos.
- Kotlin/JVM isolado: 10 testes executados e aprovados, incluindo a asserção corrigida e novos cenários de exclusão de clipes. Stubs Android/Compose foram usados apenas nessa execução isolada.
- ZIP: estrutura, CRC e documentação validados.
- Metadados: `versionName=0.1.57`, `versionCode=58`, `schemaVersion=4` sem migração.

## Pendente

Executar `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleRelease` no CI com Android SDK/Gradle. Verificar importação de vídeos grandes, rotação de tela e vídeo exportado em aparelho real.
