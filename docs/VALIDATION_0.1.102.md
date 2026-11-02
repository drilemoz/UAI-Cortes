# Validação 0.1.102 — Diagnóstico das vozes Gemini

## Sintoma informado
Na janela Áudio → Gerar por IA, após importar legendas o campo mostrava “Nenhuma voz disponível”; o usuário informou que a consulta não carregava. A captura não apresenta código HTTP nem resposta da API, então não prova a causa remota.

## Implementação
- Botão Diagnóstico do catálogo exibe resultado das chamadas, etapa consultada e falhas resumidas; “Executar teste novamente” faz consulta explícita, sem enviar as legendas nem gerar áudio.
- Fallback do filtro `pt-BR` para geral quando vazio ou rejeitado com 400/422; 401/403/429 interrompem com diagnóstico sem consultas adicionais.
- A listagem conserva IDs oficiais e não seleciona vozes automaticamente. Arquivos de chave e transcrições não entram no relatório.

## Testes
- `GeminiVoiceCatalogTest`: filtro com resultado, fallback vazio, fallback 400, falha 403, paginação, resposta vazia, linhas inválidas, sanitização. Sem chamadas remotas.
- `tools/validate_source.py`, `tools/test_version_sync.py` e `tools/package_source.py` executados localmente: não equivalem a compilação Android.
- Pendentes: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease`, inspeção no aparelho e resultado real do Gemini com chave autorizada.

## Resultado local registrado
- 9 verificações de comportamento Kotlin executadas por `kotlinc` com dublês locais de `JSONObject`, `JSONArray` e API Android; validam o fluxo real de fallback/paginação no objeto `GeminiVoiceCatalog`, mas **não** substituem os testes JUnit com `org.json` real ou o build Android.
- `tools/validate_source.py` e `tools/test_version_sync.py`: aprovados.
- `GeminiVoiceCatalogTest.kt` foi adicionado ao projeto, mas sua execução completa via Gradle está pendente.
