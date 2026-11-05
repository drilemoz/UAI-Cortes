# Validação Uai Cortes v0.1.153 — Favoritos e progresso

- Base: v0.1.152. Nova versão: 0.1.153 / versionCode 154.
- Favoritos são IDs públicos armazenados em SharedPreferences, sem segredos de API.
- O progresso percentual representa somente o lote concluído; não existe percentual parcial informado pela API para um WAV individual.
- Executar testes JVM/Android em CI e verificar UI no celular; validações estáticas isoladas não substituem build Gradle.
