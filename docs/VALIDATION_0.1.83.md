# Uai Cortes 0.1.83 / versionCode 84 — revisão de exportação

## Base

Revisadas as implementações 0.1.81 e 0.1.82 a partir dos arquivos-fonte ZIP. Media3 1.10.1.

## Achados

- **Confirmado no código:** `Presentation.createForShortSide(720/1080)` antes de `OverlayEffect`, solicitado por clipe principal. Corrige o erro anterior de lado curto em retratos; o resultado real do compositor depende de teste em dispositivo.
- **Confirmado no código:** três presets passam UI -> ViewModel -> ExportEngine -> `VideoEncoderSettings.Builder().setBitrate`; GOP de 2s é solicitado, não garantido.
- **Defeito corrigido:** `VideoExportQuality` limitava durações a 24h em reserva e estimativa; um vídeo com 25h recebia a reserva de 24h e a estimativa ficava congelada.
- **Defeito corrigido:** texto de falta de espaço fixava o nome do perfil em Alta mesmo com Compacta/Máxima.
- **Limite conhecido:** espaço verificado em `cacheDir`, que não assegura capacidade livre na mídia de destino se for outro volume. O MP4 ainda pode falhar no salvamento; a falha precisa ser apresentada, não ocultada.
- **Limite conhecido:** o relatório usa métricas do `ExportResult`, não um analisador independente do arquivo final. Não comprova fidelidade visual das legendas, qualidade perceptual ou GOP efetivo.
- **Limite conhecido:** vídeos de proporções distintas, PiPs, reescala múltipla, HDR, bitrate máximo e aparelhos de baixa capacidade dependem de teste com arquivos e hardware reais.

## Validação

- `python3 tools/validate_source.py`: aprovado (estrutura e política, **não** build Android).
- `kotlinc` isolado dos três módulos de exportação e testes: **18 de 18** aprovados, incluindo 3 novos casos.
- Os testes novos executados contra `0.1.82` reproduziram **2 falhas**; contra `0.1.83`, passaram.
- Kotlin PSI: **179 arquivos** sem erros de parsing (não verifica tipos/dependências Android).
- XML: **6 arquivos** válidos; workflow YAML parseado com 11 etapas.
- Verificação aritmética independente: 6 perfis e 6.060 casos, desvio de truncamento <=1 byte.
- Gradle/Android SDK ausentes do ambiente: **sem compilação Android, APK, lint Android completo ou teste instrumentado**.
- Testes físicos de 720p/1080p, PiP, legendas e MP4 publicado continuam pendentes.
