# Validação 0.1.259 / versionCode 260

Código base: ZIP da v0.1.258. Alterados StandaloneTextToVoiceScreen, StandaloneVoiceLibrary, StandaloneVoiceTextPolicy, MainActivity; adicionados StandaloneVoiceDraftStore e StandaloneVoiceTimelineImport.

- Testes de contrato: tools/test_text_to_voice_studio_259_contract.py.
- Testes Kotlin/JVM independentes com stubs Android: propriedades WAV, compatibilidade legado, renomear/favoritar, exclusão, rascunho explícito e estimativa.
- Sintaxe Kotlin: PSI real no ambiente local.
- Validação geral: tools/validate_source.py; versionamento: tools/check_version_sync.py.
- ZIP: gerado exclusivamente por tools/package_source.py com .github/workflows/android-apk.yml e validação após extração.

Limitações: Gradle/Android completo, testes instrumentados, reprodução, UX visual e teste real de síntese/API não realizados neste ambiente; apenas o GitHub Actions e aparelho podem confirmar.

Não há migração destrutiva: WAVs antigos continuam reconhecidos. Metadados por sidecar e rascunhos privados são opcionais; cópia de mídia independente para a timeline.
