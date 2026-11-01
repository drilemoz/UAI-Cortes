# Uai Cortes v0.1.90 — Verificações

## Alterações
- Corrige preservação de sombra nos presets sem valor explícito; teste v0.1.89 reprovado no workflow 75 deve passar sem apagar a asserção.
- Barra de ferramentas compacta, exportar legendas com CreateDocument, exclusão total confirmada e desfazer enquanto a lista estiver vazia.
- Geração Gemini por arquivo WAV extraído de cada clipe, envio somente após confirmação do usuário, horários em anotações `word_info` e revisão prévia de resultados. Dados temporários limpos em finally.

## Testes
- `python tools/validate_source.py`.
- `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no GitHub Actions; nenhum APK local foi compilado.
- Testes manuais: importar/exportar SRT/TXT (UTF-8, caracteres acentuados), remoção/Desfazer, todas as ações em tela estreita, substituir/adicionar, consentimento/cancelamento Gemini, chave ausente, erro 429 e indisponibilidade de modelo, sem áudio e cortes com lacunas.
- Conferir `word_info` em resposta real do modelo e MP4 renderizado, incluindo sincronismo de bordas de clipes.

## Pontos pendentes
- Build Android completo no runner CI e teste em dispositivo físico.
- Chamadas externas reais à API dependem de chave do usuário e respectiva cota; não foram realizadas aqui.
