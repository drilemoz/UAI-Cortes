# Validação da v0.1.288

- Abra **Legendas → Automáticas → Gerar legendas**. Confirme dois cards, com estados sem modelo ou sem chave. Nenhum card envia áudio ou começa a geração.
- Abra e recolha a explicação Gemini, marque a preferência, reabra e verifique que somente a explicação longa sumiu. O aviso resumido e o botão Enviar e gerar continuam. Rever pela boca pede autorização separada.
- Teste clipes recortados e uma narração numa faixa que começa aos 4 s e com recorte na origem. Confira que os horários individuais começam no ponto correto no app, inclusive ao gerar só um intervalo.
- Edite maiúscula, vírgula e uma palavra nova. As duas primeiras preservam os tempos. A terceira marca o trecho para conferir pelo áudio. Compare a reprodução da faixa escolhida com a prévia do clipe.
- Refaça um intervalo com uma fala ausente e confira que legendas fora dele e as que cruzam suas bordas permanecem. Em 429/cancelamento, restaure a última transcrição Gemini sem nova chamada.
- As verificações JVM cobrem recorte, tempos absolutos, pontuação, âncoras preservadas e recuperação seletiva. Testes no Android com API configurada são necessários para validar aparência, reprodução e tempo de execução.
