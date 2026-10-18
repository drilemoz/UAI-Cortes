# Uai Cortes 0.1.4: validação do pipeline de entrega

## Causa verificável no workflow da versão 0.1.3

- O gatilho `push` para `main` era permitido, mas a etapa `Publicar APK direto em GitHub Releases` tinha `if: github.event_name == 'workflow_dispatch' || startsWith(github.ref, 'refs/tags/')`.
- Portanto, o build de um push em `main` pulava a única etapa de publicação e o job poderia ficar verde sem entregar um APK.
- Não existia etapa `actions/upload-artifact`.
- Não foi anexado o log da execução relatada como aprovada; a causa é deduzida diretamente do YAML entregue anteriormente, e o evento exato da execução ainda não foi confirmado.

## Correção

- Após testes, Lint e `assembleRelease`, o workflow valida arquivo APK e assinatura com `apksigner`, além dos metadados com `aapt` (package ID, 0.1.4, versionCode 5, targetSdk 36).
- Usa `actions/upload-artifact@v4` em **todas** as execuções, com `if-no-files-found: error`.
- Usa `gh release create/upload` em **todas** as execuções sem filtro por tipo de evento.
- Valida que o APK individual está em Release Assets e gera resumo da execução.
- Artifacts são compactados pelo GitHub ao baixar; Releases fornece APK sem ZIP.

## Validações locais

- `python3 tools/validate_source.py`.
- YAML parseável, versão consistente, integridade ZIP, confirmação das duas etapas obrigatórias de entrega.
- Não há Gradle nem Android SDK no ambiente local; executar workflow e testar APK em dispositivo para comprovar implementação.
