# Validação do código-fonte · v0.1.51 / versionCode 52

- Validador `tools/validate_source.py`: aprovado.
- Parsing PSI Kotlin: 75 arquivos de produção/teste, zero erros sintáticos.
- Kotlin/JVM isolado: cinco verificações do indicador de progresso, incluindo porcentagem nula e limites.
- Integridade ZIP: conferida após geração.
- **Não realizado:** build Gradle Android, testes instrumentados/UI no celular, arrasto real, exportação MP4 de fotos e compatibilidade nas diferentes arquiteturas.
- Testar no GitHub Actions: `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease`, confirmar cinco APKs; depois testar no aparelho com 3 e 15 imagens e exportação/retorno.
