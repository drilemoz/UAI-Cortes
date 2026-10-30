# Refatoração 0.1.62: estado de exportação

- `ExportTaskCoordinator` (sem Android): máquina de estados e token monotônico por tentativa, prevenindo callbacks atrasados.
- `ExportTaskViewModel` (`AndroidViewModel`): gerencia o `ExportEngine`, polling de progresso com `viewModelScope`, e o resultado salvo; resiste à recriação da composição.
- `ExportScreen`: recebe estados observáveis, pede exportação/cancelamento, conserva prévia e opções de qualidade. Não cria nem cancela motor durante `DisposableEffect`.
- `ExportEngine`: a partir do início do salvamento na galeria, cancela apenas após completar a gravação, preservando cópia e limpeza.

## Limitações
Sem serviço foreground, persistência de job nem retomada após morte do processo. O trabalho pode parar ao encerrar o processo; testes reais e Gradle/SDK36 continuam pendentes. Não é uma mudança no JSON de projeto ou no codec.
