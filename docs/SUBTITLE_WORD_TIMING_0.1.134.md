# Sincronização manual do karaokê — 0.1.134

## Escopo

- Botão **Sincronizar** em uma legenda individual; o usuário decide aplicar ou cancelar.
- Tempos existentes de reconhecedores preservados até mudança explícita, sem transmissão de mídia.
- Distribuição linear só por toque e identificada como aproximação, não reconhece voz.
- Ajuste de fronteira entre palavras adjacentes em passos de 0,1 s ou por tempo digitado. Sem ultrapassar palavra anterior/próxima; o início da primeira e o fim da última continuam no valor salvo.
- Destaque de prévia calculado por `SubtitleAnimations.frame`, como no exportador. A prévia de demonstração converte horários absolutos proporcionalmente; caso esteja corrompida, recorre ao efeito sem tempos individuais.
- Se a legenda tiver mais de 40 palavras ou duração menor que 60 ms por palavra, o ajuste manual fica desabilitado.

## Compatibilidade e limitações

- `SubtitleCue.wordTimings` e `animation="karaoke"` já existiam. Nenhuma migração JSON ou alteração de schema.
- No botão Salvar karaokê, apenas `wordTimings` e a animação mudam no rascunho. Salvar legenda ainda é necessário para persistir no projeto; cancelar não altera o projeto.
- O editor não escuta áudio nem cria alinhamento vocal real; o usuário deve ouvir no player e ajustar manualmente.
- Compilação Android completa, teste de toque no aparelho e conferência MP4 em múltiplas proporções ainda pendentes.

## Testes

- `SubtitleWordTimingEditorPolicyTest`: distribuição, alterações locais, limites, pausas, projetos antigos e rejeição de sobreposição.
- `tools/test_subtitle_word_timing_134_contract.py` no CI.
