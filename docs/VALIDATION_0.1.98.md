# Validação da v0.1.98 (versionCode 99) — 01/11/2026

## Incidente observado

- Run: Gerar APK Uai Cortes 83, etapa “Validar código-fonte e política de publicação”.
- O GitHub Actions falhou no acesso `root/app/build.gradle.kts` com `FileNotFoundError`.
- Diferença exata entre ZIP e diretório gerador da v0.1.97: `app/build.gradle.kts`, `build.gradle.kts` e `settings.gradle.kts` ausentes no ZIP; outros 294 arquivos sem divergência byte a byte.
- O workflow não chegou à compilação Android; o log não prova nenhum erro Kotlin.

## Correção

- Restaurados os três arquivos Gradle no pacote e incrementados `versionName` e `versionCode`.
- `tools/package_source.py` prepara arquivos-fonte com exclusões de build/segredos e verifica os nomes essenciais. O pacote gerado é reaberto, extraído e novamente submetido a `tools/validate_source.py`, encerrando com erro se faltar um script.
- Nenhuma alteração de lógica de interface, edição, transcrição ou TTS.

## Verificações e pendências

- `python3 tools/validate_source.py` no diretório completo e a partir do pacote extraído.
- CRC e inventário de arquivos Gradle, JSON/XML e metadados de versões do ZIP.
- Compilação Android real, Lint, testes JVM do Android e instalação física ainda pendentes. Executar workflow no GitHub com os três scripts Gradle na raiz e pasta `app/`.
