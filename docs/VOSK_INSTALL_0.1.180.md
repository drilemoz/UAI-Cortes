# Uai Cortes 0.1.180 — Instalação segura do Vosk

## Problema observado

O diagnóstico enviado anteriormente registrou falhas `IllegalStateException` em `vosk_install` e `vosk_zip_import`. O fluxo antigo interpretava diversas causas como erro genérico e extraía somente arquivos com um prefixo exato `vosk-model-small-pt-0.3/`. Não havia conferência manual do CRC dos arquivos extraídos nem rollback do destino incompleto.

Não é possível afirmar, apenas pelo diagnóstico antigo, qual ZIP exato estava sendo importado nem se a falha vinha de arquivo corrompido, estrutura não reconhecida, armazenamento ou transporte.

## O que muda

- `VoskArchiveInstaller` extrai em diretório privado temporário e valida antes de instalar. Arquivos ZIP podem conter `vosk-model-small-pt-0.3/...`, uma pasta exterior adicional `pasta/vosk-model-small-pt-0.3/...` ou conteúdo do modelo diretamente na raiz.
- Rejeita caminhos absolutos, subida `..`, entradas duplicadas, mais de 4 mil entradas ou mais de 300 MiB extraídos. Calcula CRC32 de cada arquivo e confere tamanho declarados.
- `VoskModelLayout.detect` verifica os caminhos básicos do modelo legado e moderno, **não valida o idioma ou o modelo nativo integralmente**. Um ZIP sem pasta indicativa só pode ser reconhecido estruturalmente; não assumir identificação semântica de português.
- `installValidated` nunca troca modelo já reconhecido. Guarda um destino incompleto em `.install-previous` até concluir a troca; restaura um backup válido quando identifica uma instalação anterior interrompida.
- `VoskInstallFeedback` exibe mensagens controladas e registra códigos de diagnóstico, sem divulgar nome, caminho/URI ou texto do arquivo.
- Ao cancelar, a etapa de cópia/extração observa o cancelamento da coroutine; `withStaging` remove ZIP/pasta temporária em `finally`. A substituição final não contém suspensão entre operações de renomeação.

## Validação

- Kotlin/JVM sem Android: ZIP oficial sintético, ZIP com wrapper, ZIP com arquivos na raiz, modelo moderno, rejeição de outro nome de modelo, `..`, limite, ZIP inválido, preservação de modelo válido e restauração de backup; 10 testes de verificação do núcleo e 11 testes JUnit compatíveis executados com stubs no JVM.
- `tools/test_vosk_install_180_contract.py` acompanha o código para detectar regressões de segurança e privacidade.
- Todos os scripts `tools/test_*.py` e `tools/validate_source.py` devem permanecer aprovados no CI.

## Pendente no Android

Executar Gradle `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease` e instalar APK para verificar download/importação do ZIP português oficial (aproximadamente 31 MB), cancelamento, teste de fala offline e reinício do aparelho. Não deduzir sucesso da integração nativa apenas da validação dos arquivos.
