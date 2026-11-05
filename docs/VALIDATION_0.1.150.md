# Uai Cortes 0.1.150 — Correção CI126

- **Fonte:** workflow GitHub Actions 126, 05/11/2026; compilação Kotlin Debug/Release aprovada, `compileDebugAndroidTestKotlin` aprovada; `:app:testDebugUnitTest`: 466 testes, um falhou em `ExportTaskCoordinatorTest.invalidResolutionOrFileNameAreRejected` (linha 93).
- **Causa:** o teste anterior incluía 480p entre as resoluções proibidas, embora `ExportTaskCoordinator.start()` aceite 360p, 480p, 720p e 1080p desde a v0.1.147.
- **Correção:** atualizar apenas os testes e as versões. Em JUnit, 480p continua válido; valores -1, 0, 240 e 2160 devem falhar. Adicionado teste positivo de todas as resoluções e verificação estática cruzando UI/coordenador/JUnit.
- **Preservação:** nenhum ajuste de renderização, Media3, codecs, API Gemini, configurações de chaves, exportação real ou `schemaVersion=4`.
- **Validação local:** testes Python, `check_version_sync`, sintaxe Kotlin e teste JVM isolado devem ser registrados após executar; **não confundir com** `:app:testDebugUnitTest` integral ou criação de APK.
- **Próximo CI:** `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease` e validação/publicação dos cinco APKs.
