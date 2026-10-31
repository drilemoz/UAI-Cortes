# Validação Uai Cortes 0.1.72 (versionCode 73)

Mudança: opções de velocidade até 5× somente na prévia; botão de câmera ao lado dos controles salva quadro ORIGINAL do arquivo de vídeo em PNG, respeitando posição global e os cortes. PiP, legendas e transições não aparecem na captura do quadro de origem.

## Verificações locais

- `python tools/validate_source.py`: rodar após atualização.
- Kotlin/JVM isolado: `PreviewPlayback`, `FrameCapturePlan`, `TimelinePlaybackCoordinator`, `TimelinePrecision`, `ProjectNaming`, incluindo limites de cortes.
- Integridade do pacote ZIP: CRC e caminhos relativos.

## Testes Android necessários

1. Compilar `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` e `:app:assembleRelease` no CI.
2. Capturar quadros no começo, meio, fim e fronteira entre dois clipes, especialmente vídeo vertical com rotação e cortes.
3. Verificar arquivo PNG no álbum `Pictures/UaiCortes`, resolução, orientação, nome, qualidade e falha de permissão SAF sem crash.
4. Testar áudio e sincronismo da prévia nas velocidades 3×, 4×, 5× em aparelho real (decodificador pode não acompanhar).
5. Garantir que velocidade não afeta MP4 e que captura não altera o JSON de projetos (schema 4).

**Nenhuma compilação Android completa ou teste físico foi confirmado nesta entrega.**
