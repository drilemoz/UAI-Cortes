# Validação da v0.1.286

- Sem chave, abra IA do roteiro e confirme que Gemini está cinza e que Configurar API abre os ajustes. Com chave, selecione um modelo de texto. Trocar o modelo não muda a voz nem o modelo falado.
- Cole o causo do Fiat 147. Verifique a sugestão de tom local, a estimativa em m:ss acima de 60 s e a possibilidade de manter o texto ao rejeitar a revisão. Uma amostra curta só é sintetizada ao confirmar.
- Em um projeto com clipes de durações diferentes, teste 7 s, 10 s e Clipes do projeto. Compare as janelas e o WAV final, conferindo que os intervalos entre falas não deslocam as cenas. Uma fala excedente não permite montar antes de corrigir.
- Interrompa depois de um trecho pronto; reabra a tela e restaure o lote. O progresso deve voltar e a retomada deve mandar apenas as pendências. Simule 429 sem informação diária, cota diária no app e QuotaFailure PerDay.
- Ouça cada WAV, confirme duração e exportação SAF e que inserir um WAV feito para os clipes começa em 00:00 mesmo com cursor avançado. Projetos antigos e WAVs livres conservam o comportamento original.
- Testes JVM verificam classificação, formatação, cortes por palavra, retomada, WAV/silêncio e confirmação de cota diária. A resposta da API Gemini e a sincronização visual com o vídeo exigem credencial e Android.
