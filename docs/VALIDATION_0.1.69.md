# Validação v0.1.69 / versionCode 70

## Implementado

- Instalação alternativa do modelo Vosk via seletor Android e ZIP oficial baixado previamente.
- Download interno com contador de bytes, progressão coerente e diagnósticos de falhas HTTP, DNS e timeout.
- Extração e instalação compartilhadas, com limites de tamanho, verificação da estrutura e bloqueio de caminhos que saiam do diretório temporário.
- Regra de instalação exclusiva via Mutex, evitando duas operações sobre o mesmo ZIP temporário.

## Testado

- Validador estrutural `tools/validate_source.py`.
- Análise sintática Kotlin via parser PSI.
- Compilação e execução isoladas da lógica de percentual/bytes.

## Ainda pendente

- Download HTTP real no celular e importação real pelo seletor Android.
- Compilação Gradle completa, testes JUnit e validação da biblioteca nativa Vosk no aparelho.
- Não foi comprovada a causa da conexão anterior permanecer em 0%.
