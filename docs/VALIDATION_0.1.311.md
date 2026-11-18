# Validação da 0.1.311 — Recuperação Gemini

- Base 0.1.310. SDK 36, versionCode 312, schema e cinco variantes Android preservados.
- Revisão, descrição, roteiro e análise visual podem tentar um modelo alternativo em HTTP 503 ou HTTP 404 da rota específica do modelo. A pausa é cancelável e nenhuma solicitação tem mais de uma troca automática. O modelo escolhido nos ajustes, a chave do pedido que falhou e a modalidade são preservados.
- Voz avulsa pode trocar Flash TTS por Flash-Lite TTS, mantendo o ID da voz. Não troca de Lite para Flash. Amostras em cache e geração com fila retomável continuam vinculadas ao modelo exato. Em Vozes IA, o WAV guarda o modelo real e continua reconhecido para o pedido original, sem precisar gerar de novo.
- Transcrição com palavras e diarização só emite diagnóstico. Não tenta usar texto genérico como transcrição. Lote de texto só troca se recebeu HTTP 404 na criação referenciada ao modelo. Não reenvia em 5xx, timeout, resposta inválida ou resultado incerto. Mantém o registro local pré-envio e o modelo realmente aceito.
- Em telas cobertas, o botão de relatório grava localmente operação, modelo público ou rótulo genérico, tipo de falha, HTTP e tempo. Não guarda textos, prompts, credenciais, URLs, IDs de jobs, nomes de projeto, mídia ou mensagens brutas de exceção.
- Validação local: compilação Debug, compilação dos testes instrumentados, 776 testes JVM do app, 8 do núcleo de legendas e 176 verificações de fonte. Os seis testes de recuperação simulam 503 seguido de sucesso ou falha, ausência de resposta, limites, cancelamento, modalidades e privacidade. O ensaio em aparelho com conta Gemini de teste permanece pendente.

## Exercício de aceitação

1. Rodar Corrigir português e descrição com o modelo padrão. Induzir 503 numa conta de teste ou transporte simulado. Confirmar aviso de troca, no máximo dois pedidos e manutenção dos ajustes.
2. Simular timeout após envio e cancelar durante a pausa. Confirmar que não ocorre outro pedido. Exportar relatório em uma falha final e comparar os códigos com a simulação.
3. Validar WAV da mesma voz após fallback e reabrir o rascunho de Vozes IA para conferir reaproveitamento. Testar lote com modelo ausente sem manter um job duplicado.
4. Conferir que as legendas importadas em SRT e a saída MP4 conservam a política de uma linha e os horários.

## Referências

- [Códigos e política de espera do Gemini](https://ai.google.dev/gemini-api/docs/troubleshooting)
- [Flash-Lite multimodal](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite)
- [Síntese de voz Gemini 3.8](https://ai.google.dev/gemini-api/docs/speech-generation)
- [Processamento em lote](https://ai.google.dev/gemini-api/docs/batch-api)
