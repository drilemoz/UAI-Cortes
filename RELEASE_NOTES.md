# Uai Cortes v0.1.4 (alpha, correção de entrega do APK)

- Corrige erro de distribuição encontrado após execução aprovada: na versão 0.1.3, um `push` em `main` pulava a publicação porque a etapa aceitava somente `workflow_dispatch` ou tags; não existia etapa `upload-artifact`.
- Toda execução iniciada por push em `main`, tag `v*` ou manual publica o APK no GitHub Releases e também disponibiliza um artifact na execução de Actions.
- O download direto em **Releases > Assets** será `UaiCortes-v0.1.4.apk`. Em Actions > Artifacts, o GitHub entrega automaticamente um ZIP com o APK, e não é possível alterar esse comportamento de download da plataforma.
- O workflow verifica assinatura, package ID, versionCode=5, versionName=0.1.4 e targetSdkVersion=36 antes da publicação; falha se não encontrar o APK ou se a publicação/validação da Release falhar.
- Adiciona resumo com link direto da Release, instruções para localizar o artifact e SHA-256.
- Mantém API 36, Media3, interfaces, ícone, Secrets de assinatura e verificações Lint/testes.
- Ainda não houve build desta versão no GitHub Actions neste pacote; testar o APK instalado e a exportação em dispositivo real. Dissolvência/fade e corte fino do áudio seguem não implementados.
