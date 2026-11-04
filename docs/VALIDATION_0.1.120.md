# Validação da versão 0.1.120

## Escopo
- Atualização unificada Media3 1.10.1 → 1.11.1 (cinco artefatos).
- Instrumentação de tempo do Transformer e cópia MediaStore separadamente.
- Otimização de buffer de cópia (256 KiB); conserva rollback e confirmação.
- Política conservadora para trim: NÃO usar MP4 edit list, porque pode manter dados apagados. Sem trim experimental no pipeline atual.

## Antes de publicar
1. Executar no GitHub Actions `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug`, `:app:assembleRelease`.
2. Executar `ExportPipelineSmokeAndroidTest` em Android API 26+, incluindo legenda, PiP, faixas de áudio, entrada 9:16 e transições.
3. Comparar resolução, volume, duração, legendas e sobreposição PiP entre versões anteriores e 0.1.120.
4. Medir 3 vezes o mesmo projeto (duração, bitrate, FPS, tempo de codificação e cópia) em 720p e 1080p; registrar modelo, temperatura e alimentação. Nenhum ganho foi medido neste ambiente.
5. Conferir os cinco APKs assinados e upgrade do aplicativo sem perda dos projetos.

## Estado
Contratos Python e JUnit adicionados; execução Android física não verificada nesta sessão. O repositório GitHub remoto consultado em 03/11/2026 estava na versão 0.1.117; este ZIP parte da 0.1.119 entregue na conversa.
