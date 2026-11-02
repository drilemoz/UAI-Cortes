# Validação 0.1.103 — Escolher voz e retornar ao vídeo

## Comportamento
- Confirmar voz válida na janela de seleção retorna ao editor, fecha os dois diálogos e retoma a prévia do cursor existente.
- Cancelar ou tocar fora da seleção apenas fecha o seletor, sem navegar ou gerar áudio.
- Estado de voz/texto/modelo/estilo acompanha o projeto em memória entre as telas, não guarda chave.
- A nova voz só aparece no vídeo depois de solicitar a síntese e confirmar a adição à timeline.
- Se o projeto não contiver clipes reproduzíveis, não forçar player.play().

## Testes locais
- `GeminiVoiceChoiceTest`: seleção válida, ID ausente/cancelado, catálogo preservado.
- `tools/test_voice_return_contract.py`: confirma que os dois níveis de diálogo são fechados, que só a confirmação navega, que a prévia retoma após `seekProjectPosition`, e que não há síntese/adição na seleção.
- `tools/validate_source.py`, `tools/test_version_sync.py`, `tools/package_source.py` e verificação do ZIP extraído.
- Pendente: build completo Android, lint e teste em aparelho.
