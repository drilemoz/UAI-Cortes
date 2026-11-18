# Validação do Uai Cortes 0.1.313

- O catálogo do Google lista Gemini 3.5 Transcribe e Gemini 3.5 Flash-Lite no nível gratuito. O app usa a chave selecionada e não sabe o status de faturamento do projeto. Uma chave de projeto pago segue as condições daquele projeto.
- O Transcribe mantém horários por palavra. O Flash-Lite lê o mesmo áudio e estima os horários por frase via saída JSON. Com a resposta alternativa, não guardar falantes, tempos por palavra nem aplicar animação de palavra automática.
- Uma tentativa de Flash-Lite só em HTTP 404, 429 ou 503 da etapa Transcribe. Não trocar chave, fazer novo upload ou usar modelo Pro. Em timeout, autenticação, erro relacionado a arquivo e formato de resposta inválido, apresentar diagnóstico.
- O teste sintético usa o mesmo caminho de seleção, sem vídeo nem microfone. Conferir no celular com uma chave real. O pacote não valida a cota nem o acesso de projetos reais.

Referências: https://ai.google.dev/gemini-api/docs/pricing e https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite

## Executado

- Compilação do aplicativo e do código dos testes instrumentados com SDK 36.
- 791 testes JVM sem falhas, incluindo payload, horários por frase e regras de troca.
- Validador de versões, validador de fonte e 178 scripts de contrato sem falhas. CI70 usou sua etapa estática.
