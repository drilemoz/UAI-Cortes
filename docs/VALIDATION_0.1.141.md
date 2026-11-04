# Validação v0.1.141 — CI118, URI Gemini em teste JVM

## Evidência do workflow 118
- Compilação Kotlin Debug e Release concluída, com avisos de API depreciada não fatais.
- `:app:testDebugUnitTest`: 456 testes, 1 falha (`VoiceIaPolicyTest.insertAndReplaceKeepOnlyOneVoiceWithoutTouchingMusic`, linha 24), `java.lang.RuntimeException`.
- O caminho acionava `GeminiAudioFiles.uri(File(...))`; a implementação antiga chamava `android.net.Uri.fromFile()`, que não funciona normalmente em testes JVM com `android.jar` simulado.
- `:app:assembleRelease` não terminou; nenhum dos cinco APKs foi confirmado como produzido.

## Ajustes
- `GeminiAudioFiles.uri()` passa a construir `file:///...` com `java.net.URI("file", "", caminhoAbsoluto, null).toASCIIString()`; não invoca `android.net.Uri` em JVM.
- Mantida equivalência de URIs e codificação de caminhos com espaços/acento.
- `VoiceIaPolicyTest.geminiFileUriCanBeBuiltWithoutAndroidAndEscapesUnicode` cobre o caso diretamente.
- `tools/test_ci118_voice_uri_contract.py` impede reintrodução da API Android e verifica a presença do teste.
- `versionName=0.1.141`, `versionCode=142`, workflow, metadados e documentação sincronizados.

## Validações e pendências
- `python3 tools/validate_source.py` e todos os `tools/test_*.py`.
- `python3 tools/ci70_kotlin_visibility_test.py` e `python3 tools/ci72_media3_optin_test.py`.
- Pacote ZIP exclusivamente via `tools/package_source.py`, incluindo `.github`.
- Pendente: teste JUnit Android real pelo Gradle, cinco APKs de Release, interface, áudios reais e exportação em dispositivo.
