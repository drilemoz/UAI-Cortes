# Validação local — v0.1.215 (versionCode 216)

## Alterações verificadas
- Presets de 9,5 s, 10 s, 30 s, 60 s, 1 min, 1min05s, 2 min, 2min59s, 3 min, 4 min e 8 min, além de duração personalizada HH:MM:SS.
- Duração customizada estrita entre 00:00:01 e 23:59:59; limita 180 partes antes de gerar qualquer lista; última parte pode ser menor.
- Prévia exibe horas e frações de segundos; responsividade em <330 dp/fonte 1,3×. Não foram alterados o motor Media3, saídas SAF/MediaStore e etapas de cancelamento.

## Testes realizados
- `python tools/validate_source.py` aprovado.
- `python tools/check_version_sync.py` aprovado, Gradle/CI/Novidades/metadata em 0.1.215 e versionCode 216.
- 109 scripts `tools/test_*.py`, 109 aprovados, 0 falhas.
- Kotlin PSI: 4 arquivos (política, tela, notas e teste) analisados, zero erros sintáticos.
- `kotlinc` compilou QuickVideoSplitPolicy com verificador JVM independente; 70 verificações de durações, intervalos, limite e horários aprovadas.
- A íntegra do ZIP e o teste de extração são verificados no empacotamento.

## Pendências
- **Não houve compilação Gradle/Android**, já que esta fonte não inclui o `gradle-wrapper.jar` e o ambiente não oferece Gradle/SDK. Sintaxe PSI não verifica resolução de símbolos Compose nem compatibilidade Media3.
- Validar no Android via GitHub Actions `:subtitle-core:test :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`, assim como testes físicos de duração personalizada, 9,5 s, 8 min, áudio separado, vídeo de várias horas, cancelamento e espaço livre.
- Presets de redes são orientações de duração; **não constituem certificação de monetização ou compatibilidade de publicação**.
