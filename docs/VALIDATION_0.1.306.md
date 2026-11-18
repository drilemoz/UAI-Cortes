# Uai Cortes 0.1.306 — Correção do build 262

Base 0.1.305. versionCode 307.

No build 262, as etapas compileDebugKotlin e compileReleaseKotlin não relataram erros. A etapa :app:testDebugUnitTest executou 759 testes, com uma falha em SrtSpeechAlignmentTest.kt:61. O teste esperava contorno 2 e depois sombra 0 para o karaokê rosa. O modelo de v0.1.305 passou a usar contorno 4 e sombra 1. A etapa de publicação não foi concluída.

Separados os contratos de SRT (14 sp importado e tamanho salvo preservado) e do karaokê rosa (20 sp de fábrica, contorno 4, sombra 1 e tamanho explícito de 14 sp respeitado). O aplicativo conserva os estilos e o encaixe de voz da versão anterior.

A verificação local inclui os scripts de regressão, a sincronia de versão e a validação do ZIP extraído. O ambiente não dispõe da distribuição Gradle nem do wrapper JAR. A confirmação dos testes JUnit, lint, APKs e release depende de uma nova execução do workflow Android.
