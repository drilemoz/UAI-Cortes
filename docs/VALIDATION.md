# Validação da versão 0.1.0

## Verificações realizadas ao gerar o fonte
- Estrutura de projeto Android com `settings.gradle.kts`, `app/build.gradle.kts`, Manifest e recursos: verificada.
- `compileSdk=36`, `targetSdk=36`, `versionCode=1`, `versionName=0.1.0`: verificados.
- Ícone gerado e incluído em cinco resoluções mipmap: verificado.
- Arquivo de workflow para produzir `UaiCortes-v0.1.0.apk` diretamente na Release: verificado estaticamente.
- Parser Kotlin usado localmente para detectar erros de sintaxe. Dependências Android ausentes nesse ambiente; não valida tipos/APIs.
- Projeto final ZIP e integridade dos arquivos: verificados após empacotamento.

## Pendências obrigatórias antes de distribuir APK
1. Executar GitHub Actions ou Android Studio com API 36 para confirmar build, Lint e testes JUnit.
2. Validar assinatura de Release (Secrets opcionais no build, obrigatórios para manter o mesmo certificado em atualizações).
3. Testar exportações reais com duas fontes, PiP imagem/vídeo, áudio em paralelo e codificadores H.264/AAC.
4. Testar prévia PiP em telefones reais; pode haver sincronização aproximada.
5. Implementar transições fade/dissolvência: Media3 não fornece crossfade de composições diretamente.
6. Melhorar controle de áudio por segmento com corte fino e UI para selecionar exatamente o trecho.
7. Validar retomada de exportação ao sair do app, casos de pouco armazenamento, alta temperatura e arquivos não suportados.

**Estado:** código-fonte alpha fornecido; APK compilado não foi gerado neste ambiente.
