# Validação da v0.1.287

- Abra **Legendas → Automáticas → Sincronia labial · Gemini**. Sem chave, o botão de análise fica inativo. Cancelar antes de analisar não envia dados.
- Teste com uma pessoa ou personagem em primeiro plano. Experimente adiantar o início da legenda em 200 ms. Ao analisar, confira que a sugestão aponta para um quadro dentro de 300 ms do horário original. Compare os quadros na revisão antes de decidir.
- Com karaokê temporizado, tente obter sugestão depois da primeira palavra ou antes da última. O app não deve excluir nenhuma palavra nem mudar seu horário, e um limite inseguro deve ser mantido.
- Confirme só uma sugestão de um lote. Ao usar Desfazer, confira horários, cores, estilos, texto e personagem.
- Verifique clipes recortados com rotação no Android 26 e em versão recente. Cubra câmera parada, rosto pequeno, PiP, corte e transição, 429, resposta inválida e cancelamento. Quadros ambíguos não devem causar alteração.
- Os testes JVM exercitam política e parser com entradas sintéticas. A precisão do modelo e a decodificação de quadros não são verificadas por teste estático. Não se trata de leitura de palavras pelos lábios.
