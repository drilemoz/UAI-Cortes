# Validação v0.1.117 (versionCode 118)

## Escopo

- Tela pós-salvamento mais compacta, miniatura de MP4 via MediaStore quando disponível e detalhes técnicos recolhidos.
- Campos de edição e cópia: descrição acima de 5 hashtags, até 100 caracteres incluindo espaços e quebra de linha; sem emojis/emoticons. Compartilhamento envia `EXTRA_TEXT` somente quando válido; app receptor pode ignorá-lo.
- Cliente Gemini sob demanda; chave Android Keystore existente. Envia somente contexto textual revisado no diálogo de confirmação. Sem envio do MP4 e sem geração automática.
- Fluxo de Animar Imagens preservado, com informação manual do assunto quando não há legendas.

## Validação local

- `python3 tools/test_video_saved_caption_contract.py`
- `python3 tools/check_version_sync.py`
- `python3 tools/validate_source.py`
- `python3 tools/test_version_sync.py`
- `kotlinc` + casos da política VideoCaptionPolicy (sem Android).

## Pendente obrigatoriamente

- Compilação real Android/Kotlin e testes JVM via Gradle/CI.
- Testar aparência em tela pequena/com teclado e retorno ao editor nos dois fluxos.
- Testar chamada real do Gemini com chave válida, 400/403/429/timeout e cancelamento.
- Verificar miniatura e acesso MediaStore Android 8–16; abrir/compartilhar MP4 e legenda em diferentes redes sociais (EXTRA_TEXT não é garantia).
- Confirmar no aparelho que MP4, projeto e legendas originais não foram modificados.
