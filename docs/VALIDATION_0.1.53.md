# Uai Cortes 0.1.53: matriz de validação

## Realizado no ambiente de edição
- Integridade dos arquivos do projeto e versão `0.1.53` / `versionCode 54`.
- `python3 tools/validate_source.py`: verificação estrutural das telas e recursos preservados.
- Análise de sintaxe dos arquivos Kotlin com parser PSI Kotlin 1.9.0 (sem Android SDK).
- Acrescentados casos JUnit para `ProjectCollectionCodec` (serialização e fail closed), ainda não executados pelo Gradle.

## Pendente em CI e dispositivo
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease` (cinco variantes ABI/universal).
- Abrir projetos salvos antes da refatoração; testar cortes, áudio, PiP, legendas, transições, marca d'água e retorno entre telas.
- Exportar MP4 nas duas ferramentas, cancelar renderização e provocar falta de espaço/permissão durante a cópia para checar rollback no MediaStore.
- Validar prévia/render de MP4, imagens verticais/horizontais e rotação no celular.
- Verificar comportamento de projetos com JSON inválido e backup válido, sem apagar os arquivos originais.
- Testar MediaStore nos Androids 8/9 e versões atuais. O código mantém a estratégia de gravação da versão anterior, ainda sem confirmação física nas versões anteriores ao Android 10.

O arquivo ZIP entregue contém **código-fonte**, não um APK compilado.
