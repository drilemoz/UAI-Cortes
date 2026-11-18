# Validação do Uai Cortes 0.1.312

- A API oficial descreve o modelo Gemini 3.5 Transcribe para transcrição de arquivos (sem streaming), com entrada de áudio diretamente ou por URI, e horários por palavra. Esta versão usa Interactions com chave ativa e sem streaming.
- Transcrição do projeto mantém pt-BR, horários por palavra e diarização opcional. O WAV normalizado até 2 MiB segue direto, sem a etapa Files. Acima do limite, mantém upload e espera da ativação. Não há reenvio por troca de formato depois de falhar.
- A amostra sintética tem 1,785 s, mono PCM de 16 kHz. O teste verifica a chave ativa, pede horários por palavra, não usa microfone e não lê o projeto.
- Em Android, verificar que o padrão permanece após fechar o app, somente abre o painel Gemini se houver chave e nunca inicia envio sem confirmação. Confirmar que Vosk, revisão de português e síntese de voz não mudaram.
- Em dispositivo conectado, testar o botão com a chave real, depois um projeto longo e outro curto. Em caso de falha, conferir etapa, HTTP quando presente e exportar o relatório. Não foi feita chamada real com a chave do usuário nesta entrega.

Documentação consultada: https://ai.google.dev/gemini-api/docs/transcribe

## Executado nesta entrega

- Compilação Debug e compilação dos testes instrumentados com SDK 36.
- 787 testes JVM sem falhas (779 do app e 8 de subtitle-core).
- Validador de versões e de fonte, e 177 scripts de contrato sem falhas. O script CI70 usou sua etapa estática.
