# Validação v0.1.250 (2026-11-09)

- Escopo: leitura manual de posts de Páginas Facebook com anexo de vídeo, sem promessa de consulta universal de Reels.
- Preservados Google/YouTube, projetos schema 4, SocialStore schema 1, credenciais só em memória e cinco variantes.
- API Meta Graph v26: GET /{page-id}/posts com anexo de vídeo; requer permissões de leitura aprovadas. Pode omitir Reels e miniaturas.
- `tools/check_version_sync.py`: aprovado, v0.1.250/versionCode 251.
- `tools/validate_source.py`: aprovado (contratos estruturais).
- 16 testes `tools/test_social*py`: aprovados, incluindo o novo contrato de consulta Meta, autorização e proteção de URLs.
- Análise Kotlin PSI: 456 arquivos `app/src` e `subtitle-core/src`, zero erros sintáticos.
- `FacebookPageVideoPosts250AndroidTest`: três cenários instrumentados adicionados, **não executados** sem Android.
- Gradle Android: tentativa pelo wrapper devolveu "Gradle não está instalado"; não é build aprovado.
- Arquivo ZIP: validação CRC e extração será realizada no empacotamento.
- Build Gradle Android, login Meta real, paginação real e teste visual no aparelho ainda não comprovados neste ambiente.
