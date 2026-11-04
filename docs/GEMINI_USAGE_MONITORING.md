# Consumo Gemini no Uai Cortes 0.1.131

## Implementado no código-fonte

- `GeminiUsagePolicy` lê `usageMetadata` para `generateContent` e `usage` para `interactions`. Campos: entrada, saída, raciocínio, cache e total oficial, todos opcionais. Nunca computar `total` pela soma de campos, porque alguns são subtotais.
- `GeminiUsageLedger`: grava somente data, projeto configurado, modelo, tipo de operação, status HTTP simplificado e números. Não grava chave, texto, imagem, áudio, URLs, resposta da API ou custos. Histórico privado local: 500 eventos/90 dias, sem serviço próprio de sincronização.
- Captura em `GeminiVideoCaptionClient` (descrição/Flow), `GeminiTtsClient` (narrativa incluindo amostras) e `GeminiSubtitleClient` (transcrição). Em failover, cada tentativa 429 é atribuída ao projeto realmente usado, e sucesso é registrado separadamente.
- `GeminiUsagePanel`: Configurações > Google Gemini, períodos 24h/7d/30d e agrupamento por projeto/modelo, avisos sem contagem, botão limpar e acesso ao painel Google AI Studio.
- `GeminiUsageHint`: alerta antes de cada solicitação; texto recebe uma faixa **local** aproximada sem computar tokens adicionais do prompt nem resposta. Conteúdos de áudio não recebem previsão fictícia.
- Em descrição/Flow, `models.countTokens` somente por botão; usa o prompt completo da própria operação, sem gerar resposta ou acionar failover. A ação envia texto ao Google de forma explícita.

## Limites que devem permanecer claros

As cotas Gemini são por projeto Cloud, não por chave. A disponibilidade, limites reais RPM/TPM/RPD, saldo de créditos e cobrança devem ser consultados no AI Studio. HTTP 429 é limite e NÃO demonstra que o usuário ficou sem créditos monetários. Preços dependem de modelo, modalidade, plano, cache e data; na versão 0.1.131 o custo é **indisponível**, não R$ 0 ou US$ 0.

O histórico representa exclusivamente chamadas deste aplicativo neste aparelho. Projetos sem identificador configurado aparecem como `Projeto não informado`, sem inferir sua identidade. Pausar um projeto é ação da política de failover, não controle de gastos real.

## Pendências de validação

- Compilar Android Debug/Release e AndroidTest no GitHub Actions; não interpretar análise sintática como build.
- Verificar `countTokens` e `usageMetadata` com chave real autorizada e modelos válidos.
- Verificar interações de transcrição e TTS em Android; consumo da API pode mudar conforme modelo.
- Conferir rodapé de tokens, painel com fontes grandes, troca de projeto após 429, modo offline e histórico sem contagem.

## Fontes oficiais (consultadas em 04/11/2026)

- https://ai.google.dev/api/generate-content
- https://ai.google.dev/api/interactions-api
- https://ai.google.dev/api/tokens
- https://ai.google.dev/gemini-api/docs/rate-limits
- https://ai.google.dev/gemini-api/docs/billing

## Correção de compilação 0.1.132 (CI110)

O workflow 110 da versão 0.1.131 apontou dois erros Kotlin na interface de histórico e na contagem sob demanda. A correção importa `horizontalScroll` e rotula o retorno de `countInput` com `return@withContext`. A API, semântica de consumo e privacidade não foram alteradas. O CI precisa compilar Debug e Release antes de considerar o recurso aprovado no Android.
