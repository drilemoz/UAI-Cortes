# Validação v0.1.124 · Etapa 6

## Escopo

Destinos Original / Shorts / Reels / TikTok; FIT/CROP 9:16; compressão recomendada e resolução solicitada versus obtida.

## Testes de contrato e unidade

- `python3 tools/check_version_sync.py`
- `python3 tools/validate_source.py`
- `python3 tools/test_social_export_contract.py`
- `python3 tools/test_*.py` (executar cada arquivo individualmente)
- `SocialExportPolicyTest` e `ExportQualityReportTest` na compilação de testes Android/JVM.

## Testes instrumentados pendentes

- `ExportPipelineSmokeAndroidTest.exportSocialPortraitWithFitProducesPortraitMp4`: fonte horizontal 16:9 convertida para 720×1280 com barras, sem distorção.
- `ExportPipelineSmokeAndroidTest.exportSocialPortraitWithCenterCropProducesPortraitMp4`: fonte horizontal 16:9 convertida para 720×1280 com corte central.
- Conferir em aparelho físico a sincronização de legendas, bordas visuais e PiPs; testar duas orientações de origem, áudios e transições.
- Validar Gradle assembleDebug, compileDebugAndroidTestKotlin, lint, todos os cinco APKs assinados no GitHub Actions.

**Limitação:** testes Python e compilação Kotlin isolada não provam a compilação do app Android nem fidelidade em vídeo real. Não há garantia de aceitação automática por plataformas nem ganho de qualidade/velocidade.
