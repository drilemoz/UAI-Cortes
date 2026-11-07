# Validação local — v0.1.216 (versionCode 217)

## Alterações revisadas
- Presets e duração personalizada da v215 preservados; alternar entre Segundos e Minutos agora restaura a última duração escolhida em cada grupo.
- Campos HH/MM/SS empilhados quando a largura é inferior a 330 dp ou o fontScale >= 1,3.
- A gravação de MP4 em `GalleryVideoSaver.save` é cooperativamente cancelável, verifica cancelamento entre blocos de 256 KiB e antes de retirar `IS_PENDING`; tenta apagar a saída pendente ao cancelar ou falhar.
- `QuickVideoSplitEngine.activeGeneration` marcado `@Volatile`; callback da gravação antiga não altera `saving` nem `temp` após iniciar outra tarefa.

## Testes
- 110 scripts `tools/test_*.py`: 110 aprovados, zero falhas, após atualizar contrato legado de cópia local.
- `python tools/validate_source.py`: aprovado.
- `python tools/check_version_sync.py`: aprovado.
- `kotlinc` compilou `QuickVideoSplitPolicy` e runner JVM: 70 asserções aprovadas.
- Parser PSI dos quatro arquivos Kotlin revisados: sem erro sintático.
- Integridade do ZIP e extração verificadas após empacotamento.

## Pendências
- O Gradle completo e teste Android Media3/MediaStore continuam pendentes; este pacote não contém `gradle-wrapper.jar` e o ambiente não tem distribuição Gradle/SDK.
- Testar cancelamento durante a cópia de MP4 grande, cancelamento perto da publicação, seleção de durações, arquivos resultantes e legibilidade em telas estreitas. Cancelamento não garante interrupção imediata de uma operação de I/O do provedor travada nem persistência após morte do processo.
