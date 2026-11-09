# Validação v0.1.251 (2026-11-09)

## Diagnóstico do workflow 208

O workflow `Gerar APK Uai Cortes` (execução 208) parou antes de `Testes, Lint e compilar Release`, na validação Python local. `tools/test_about_whatsnew_207_contract.py` executava `home.index('"Novidades","Mudanças da versão atual e histórico"')` e falhava com `ValueError: substring not found`, por exigir um atalho removido deliberadamente da tela Configurações na v0.1.248. Não há evidência neste workflow de falha Gradle/Kotlin: essa etapa não foi executada.

## Correções

- Atualizado contrato 207 para verificar que Configurações não contém mais o item Novidades, mas a abertura automática da página após atualização continua funcionando; a página mostra somente mudanças atuais; os dados de histórico permanecem em `AppReleaseNotes` sem aparecer na apresentação.
- O contrato de consulta Meta criado para v0.1.250 passa a aceitar futuras versões sequenciais, sem afrouxar testes de token temporário, permissão, URLs e cursor.
- Uma varredura completa dos 142 contratos revelou também `test_updates_201_contract.py` obsoleto: ainda cobrava um `showHistory` que saiu da interface na v0.1.248. Corrigido para testar a interface atual sem alterar o gerenciador de atualização.
- `RELEASE_NOTES.md` segue o formato exigido pelo gerador CI128: `# Uai Cortes · versão <versão>`; garante extrair apenas o bloco da versão atual ao publicar no GitHub.
- Versões sincronizadas em Gradle, CI, metadados, AppReleaseNotes, ferramentas e documentação. Sem alterações em dados de projeto, fluxo de publicação, OAuth nem código Media3.

## Verificações

- `tools/check_version_sync.py`: aprovado, 0.1.251/versionCode 252.
- `tools/test_about_whatsnew_207_contract.py`: aprovado.
- `tools/test_social_facebook_pages_250_contract.py`: aprovado.
- `tools/test_ci128_release_notes_contract.py`: aprovado.
- `tools/test_updates_201_contract.py`: aprovado após correção da exigência obsoleta.
- `tools/test_social_redesign_248_contract.py`, `tools/test_social_visual_249_contract.py`: aprovados.
- `tools/release_notes_for_github.py`: aprovado, notas da versão atual com 869 bytes UTF-8.
- `tools/validate_source.py`: aprovado.
- Bateria `tools/test_*.py`: 141 passaram, 1 falhou por contrato obsoleto; o contrato foi corrigido e reexecutado com sucesso, totalizando 142 aprovados sob limite de até 3 segundos por teste (não equivale à execução sem limite do CI).
- Compilação Android/Gradle completa, lint Android e execução em aparelhos físicos: **pendentes**, dependem do próximo CI.
