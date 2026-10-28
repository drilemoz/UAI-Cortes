# Validação da versão 0.1.49 (versionCode 50)

## Aprovado localmente
- Parser sintático Kotlin (69 arquivos .kt incluindo testes), sem erros de parsing.
- Verificador estrutural `python tools/validate_source.py`.
- 915 verificações executadas com `kotlinc` da lógica de movimento/cor de transições.
- Integridade de todos os arquivos do ZIP.

## Não executado neste ambiente
- Compilação `:app:assembleRelease` pelo Gradle (distribuição e Android SDK indisponíveis localmente).
- Testes JUnit do projeto, inclusive os novos testes de horários Vosk e transições.
- Instalação das bibliotecas nativas Vosk nas cinco variantes de APK.
- Download do modelo e processamento real de um vídeo com áudio no celular.
- Verificação de fidelidade de zoom/pan no MP4 real exportado.

## Passos para testar no Android
1. Executar workflow GitHub Actions e verificar build Debug/Release, Lint e testes.
2. Instalar APK da ABI correta e abrir projeto com vídeo de 15 a 30 segundos com fala nítida em português.
3. Legendas → Auto → Baixar modelo. Confirmar instalação e repetir com internet desligada.
4. Transcrever, editar uma legenda, aplicar e conferir sincronização e exportação do MP4.
5. Testar vídeo cortado em dois clipes; conferir que a legenda do segundo clipe use o horário correto.
6. Testar projeto sem áudio e projeto maior que 10 minutos: apresentar mensagem, sem travar.
7. Em Transições, comparar prévia e exportação para Zoom cinematográfico, Recuo suave e Deslocamento lateral.
8. Na Home e Projetos, conferir miniaturas e pedir confirmação antes da exclusão; cancelar deve manter o projeto.
9. Revogar permissão de um vídeo e confirmar fallback de miniatura sem crash.

**Importante:** Não declarar o motor de legendas automáticas e as transições novas validados fisicamente até realizar os testes acima.
