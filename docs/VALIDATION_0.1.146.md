# Validação da v0.1.146 (versionCode 147)

Escopo: exclusivamente interface de configuração Google Gemini (credenciais, privacidade, grupo de chaves, histórico local de uso). Nenhuma modificação na API de geração nem em dados de projetos.

## Testes locais previstos
- `python3 tools/validate_source.py` e `python3 tools/check_version_sync.py`.
- `python3 tools/test_ci123_gemini_settings_compact_contract.py`.
- Suíte `tools/test_*.py`, Kotlin PSI quando disponível.
- `python3 tools/package_source.py --output ...` incluindo `.github` e validando a fonte extraída.

## Pendências de validação Android
- Executar Gradle (`:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease`) em CI.
- Verificar layout da barra em 320–400 dp, rolagem com fonte ampliada e TalkBack.
- Testar segredos: cópia com confirmação, arquivo exportado em texto legível com aviso, remoção, troca de chave, teste TTS após consentimento e alternância entre múltiplas chaves.
- Conferir indicadores sem eventos, com 429, tokens desconhecidos e filtro 24h/7d/30d.
