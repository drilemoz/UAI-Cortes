# Validação do Uai Cortes 0.1.2

## Evidência de falha, GitHub Actions #2, 18/10/2026
- O Android SDK API 36 e Build Tools 36.0.0 foram instalados e verificados.
- Os quatro Secrets estavam disponíveis e a keystore foi decodificada para assinatura persistente.
- O Gradle 8.11.1 iniciou normalmente e a tarefa `:app:compileDebugKotlin` falhou.
- Erros `Unresolved reference 'OverlaySettings'` e `Unresolved reference 'VideoCompositorSettings'` em `ExportEngine.kt`, acompanhados por dois `overrides nothing` e um `Argument type mismatch` (7 diagnósticos no total).
- O build não chegou a `:app:assembleRelease` e não gerou APK.

## Correção aplicada
- Imports substituídos pelos tipos documentados `androidx.media3.common.OverlaySettings` e `androidx.media3.common.VideoCompositorSettings`.
- `StaticOverlaySettings` continua em `androidx.media3.effect`, conforme referência Media3.
- Sincronizados Gradle, Sobre, README, workflow e notas para `0.1.2`/versionCode `3`.
- Referências oficiais: https://developer.android.com/reference/androidx/media3/common/OverlaySettings e https://developer.android.com/reference/androidx/media3/common/VideoCompositorSettings

## Validações de fonte
- Executar `python3 tools/validate_source.py` para checar imports, versões, arquivos e workflow.
- Validar sintaxe do YAML e integridade/CRC do ZIP.
- **Não houve validação do APK Android completo neste ambiente.** Repetir `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no Actions e confirmar a verificação `apksigner`.
- PiP, volume e exportação continuam dependentes de testes reais de funcionamento em aparelhos com hardware e codecs distintos.
