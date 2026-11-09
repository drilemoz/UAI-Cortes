# Validação da fonte UaiCortes 0.1.246 / versionCode 247

## Escopo

Base da 0.1.245; importação de fotos por galeria, cópia privada cancelável, detecção de MIME por cabeçalhos, classificação segura de falhas, acesso direto a Reparar mídias e tratamento de cópias provisórias não salvas. Sem mudança no schemaVersion 4, exportador, servidor de vozes ou assinatura.

## Verificações locais executadas

- `python tools/check_version_sync.py`: **aprovado** (Gradle, CI, AppReleaseNotes, README/CHANGELOG/RELEASE, JSON e empacotamento).
- `python tools/validate_source.py`: **aprovado**.
- Parser PSI do Kotlin em 429 arquivos Kotlin de produção/testes: **0 erros sintáticos**, sem comprovar compatibilidade de APIs Android.
- `kotlinc` + `java` no `ImageImportSafety.kt`, com 7 asserções para MIME, GIF disfarçado, ausências e privacidade de erros: **aprovado**.
- Contratos Python direcionados aprovados: `test_ci246_media_resilience_contract.py`, `test_main_image_clip_169_contract.py`, `test_version_sync.py`, `test_updates_201_contract.py`, `test_ci245_updates_subtitles_export_contract.py`, `test_subtitle_pink_242_contract.py`.
- Execução agregada de 138 contratos Python: avançou por 120 resultados sem falha impressa, mas **não concluiu** devido ao tempo limite da execução. Isso **não** significa 138 testes aprovados.
- Empacotamento de fonte: ZIP será validado com `tools/package_source.py` por extração, CRC e nova validação estrutural.

## Não comprovado

- **Não houve build Android completo**: este ambiente não tem Gradle/SDK/artefatos de dependências.
- **Não houve teste em dispositivo físico**, com provedores diferentes, permissions SAF ou restauro após reiniciar.
- **Não foi reproduzido o congelamento da tela** nem medida paridade visual entre legendas de prévia e MP4. A 0.1.246 não promete consertar estes dois problemas, ainda pendentes de medição.
- Compatibilidade com Media3 e Compose deverá ser confirmada no GitHub Actions, com testes de importação e reparo no aparelho antes de remover o backup.
