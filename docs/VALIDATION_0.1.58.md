# Uai Cortes v0.1.58: correção de compilação do workflow 51

## Evidência
Arquivo de logs: `UAI-Cortes-Gerar-APK-Uai-Cortes-51-logs.zip`, 30/10/2026. O `:app:compileDebugKotlin` e `:app:compileReleaseKotlin` falharam em `MainActivity.kt`, linhas 239, 242 e 261: `launch` e `CancellationException` não resolvidos e `withContext` fora de coroutine. O código-fonte tinha `Dispatchers` e `withContext`, mas não importava `launch` ou `CancellationException`.

## Correção mínima
Adicionados imports do pacote `kotlinx.coroutines` na atividade, preservando `importScope.launch`, despacho de leitura de metadados em `Dispatchers.IO` e relançamento de cancelamento.

## Regressão e limites
- `tools/validate_source.py` rejeita ausência/duplicação desses imports e das referências importantes na função de importação.
- Compilação Kotlin/JVM isolada de um trecho equivalente com `kotlinx-coroutines-core` valida a API.
- Análise sintática PSI Kotlin e integridade do ZIP.
- **Pendente:** CI Android completo `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` e os cinco APKs. Nenhum arquivo de mídia real foi exportado nesta validação.
