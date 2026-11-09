# Validação v0.1.249 · 2026-11-09

- `python tools/check_version_sync.py`: aprovado, 0.1.249/versionCode 250.
- `python tools/validate_source.py`: aprovado (contratos estruturais gerais).
- `tools/test_social_visual_249_contract.py`: aprovado.
- 15 testes Python do módulo social: aprovados após adaptar o teste legado da v227 ao botão visual novo e o teste 248 à versão corrente.
- Parser Kotlin PSI: 279 arquivos de produção analisados, 0 erros sintáticos.
- ZIP: integridade CRC e arquivos obrigatórios serão conferidos na embalagem.

**Não executados/pendentes:** build Gradle Android (distribuição Gradle e dependências ausentes no ambiente), testes instrumentados e revisão visual da tela no aparelho. Testes estáticos não comprovam integração com a API do YouTube ou a UI real. Nenhuma credencial, endpoint, esquema de dados ou lógica de postagem foi alterado.
