# Validação de código-fonte Uai Cortes 0.1.192
- `python tools/check_version_sync.py` e `python tools/validate_source.py`
- `python -m unittest discover -s tools -p 'test_*py'`
- JUnit `SubtitleSpeakerStylesTest`: herança, exceções, restauração, JSON e escopo por personagem (executar no Gradle CI)
- Parser Kotlin dos arquivos novos/alterados.
- CRC do ZIP e consistência de versão.
- Pendente: GitHub Actions com `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, builds de APK, instrumentados, inspeção visual no aparelho, comparação do vídeo original/prévia/exportação.
