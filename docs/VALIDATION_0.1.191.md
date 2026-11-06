# Validação v0.1.191 · correção do CI157

## Evidência do GitHub Actions 157
- `:app:compileDebugKotlin`, `:app:compileReleaseKotlin`, `:app:compileDebugAndroidTestKotlin` e `:app:compileDebugUnitTestKotlin` concluíram.
- `:app:testDebugUnitTest`: 614 executados, 1 falhou em `GeminiVoicePresentationTest.kt:45`. O teste exigia hash diferente por chave, em conflito com o contrato do cache persistente da 0.1.190.

## Alteração
- Atualizado JUnit para exigir identidade igual quando somente `apiKey` muda e diferente para modelo, voz e estilo.
- Algoritmo SHA-256, frase padrão, arquivos WAV e comportamento das APIs não mudaram.
- Pendentes: novo build GitHub Actions e teste no aparelho. Narrador global/por projeto fica para uma futura versão.
