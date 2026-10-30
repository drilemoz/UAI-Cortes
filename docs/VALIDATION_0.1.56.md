# Validação 0.1.56: qualidade de exportação

Mudanças analisadas: `VideoExportQuality.kt`, `ExportEngine.kt`, `ImageAnimationEngine.kt`,
`ExportScreen.kt`, `HomeScreens.kt` e `ProjectStore.kt`.

Bitrates **solicitados**: 720p = 8.000.000 bps, 1080p = 18.000.000 bps;
AAC = 192.000 bps. O codificador pode ajustar. Não foi implementado upscale inteligente,
AV1, HEVC, 4K, nem ajuste arbitrário do FPS de entrada.

Imagens pré-processadas em PNG sem perdas antes da renderização H.264.
Esse intermediário usa mais armazenamento. A verificação preventiva
considera duas cópias estimadas do MP4 (temporário e MediaStore), mais 96 MiB,
e ocorre também após cada foto preparada. Não é garantia de espaço ou tamanho final.

Validação Android/Gradle e teste com vídeos originais não executados nesta entrega.
Verificar em aparelho:
1. mesmo vídeo-fonte 0.1.55 vs 0.1.56, em 720p e 1080p;
2. propriedades reais de saída com MediaMetadataRetriever ou ffprobe (resolução, FPS, bitrate);
3. texto fino, movimento, cenas escuras e áudio, principalmente em vídeos verticais;
4. fluxo PiP, legendas, transições e projetos antigos;
5. 80 imagens com armazenamento próximo do limite e cancelamento.
