# Uai Cortes v0.1.85 — Correção do CI 71

## Diagnóstico reproduzível

Workflow 71: tarefa `:app:testDebugUnitTest` falhou, 289 testes executados, 1 falha, `WhatsNewReleaseNotesTest.reorderingOrChangingACaptionChangesTheFingerprint` linha 34. Compilações Kotlin Debug/Release concluíram. A versão 0.1.84 tem uma única `ReleaseFeature`; inverter a ordem de um item não altera o fingerprint.

## Solução

- `WhatsNewReleaseNotesTest`: a afirmação sobre ordem usa uma lista explícita com duas novidades; a mudança de título é testada sobre qualquer quantidade de itens na versão.
- Novo teste de lista de um item: inversão preserva hash, mudança de descrição altera hash.
- `AppReleaseNotes`: inserida uma entrada de release 0.1.85 verdadeira e sincronizados versão, metadados, documentação, `tools/package_apks.py` e GitHub Actions. O algoritmo SHA-256 e a política de primeira abertura permanecem inalterados.
- `tools/validate_source.py`: proteção estrutural da regressão.

## Testes

- O CI 71 **compilou Debug/Release**, mas **não gerou os cinco APKs**.
- Validador estrutural, sintaxe Kotlin e execução isolada dos testes de novidades: ver resultados do relatório da entrega.
- Pendente: executar todos os testes Gradle, lint, geração dos cinco APKs Release e validação Android real no workflow seguinte.
