# Validação 0.1.75 (versionCode 76)

## Implementado

- Editor > toque no clipe > Áudio do clipe: seletor SAF, pré-escuta, volume, aplicar/remover.
- Faixa opcionalmente vinculada por `clipId` e `clipSourceAnchorMs`, preservando `schemaVersion=4`.
- Resolver posicão e recorte em tempo global na prévia Media3, na timeline e na composição do MP4.
- Reordenar, cortar, dividir, duplicar e excluir com trilha vinculada; preservar faixas globais.
- Retorno seguro ao editor, mensagens de erro e confirmação para remoção.

## Realizado neste ambiente

- `python3 tools/validate_source.py` (verificações estáticas)
- PSI Kotlin (parser de 158 arquivos Kotlin)
- Compilação Kotlin/JVM isolada de ClipAudioTiming, ClipAudioBindings e TrimTimelineEdit com modelos de teste, 7 cenários aprovados.
- Integridade/CRC do ZIP, JSON/XML e versionamento em metadados.

## Pendente

- Gradle Android real: `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no GitHub Actions.
- Testar em aparelho seleção SAF, origem protegida, player e volume em prévia.
- Verificar o MP4 final com clipes movidos/divididos e narração vinculada; nenhum teste de renderização física foi executado.
