# Uai Cortes 0.1.303 — Áudios do aparelho

Base 0.1.302. versionCode 304. Esquema, edição e exportação preservados.

## O que mudou

- Um seletor de áudio compartilhado abre junto do fluxo atual. Mostra nome e duração, quando conhecida, do mais recente ao mais antigo. A busca ignora maiúsculas e acentos.
- Pontos integrados: áudio do clipe, faixa e música rápidas, tela de áudio do projeto, editor de áudio, Vozes IA, clonagem (somente WAV na lista), referência de sincronização e reparo de áudio.
- O catálogo lê os volumes públicos disponíveis via MediaStore, incluindo cartão SD quando montado e indexado. Não acessa pastas privadas de outros apps. No Android 13+ usa acesso a áudio. No Android 8–12 usa leitura de armazenamento.
- “Procurar arquivos” continua disponível mesmo sem permissão e usa o seletor de documentos. Para referência de sincronização, também permite vídeo. Importação de vídeos, fotos, legendas e outros documentos permanece como antes.
- Áudio salvo no clipe e áudio reconectado usam a cópia privada do projeto. Preserva a duração mínima dos cortes na verificação do reparo e não regrava o arquivo original.

## O que foi verificado

166 scripts de regressão Python passaram, além de validate_source.py, validação estática CI70/CI72 e sincronização de versões. O contrato novo cobre as rotas, a alternativa de documentos, permissões e cópias privadas. O teste JUnit de busca e filtro WAV foi incluído, mas não executado. A tentativa de compilação/lint foi bloqueada pela ausência de Gradle e de seu wrapper JAR neste ambiente.

A [prévia da tela](Previa-Audios-UaiCortes.png) é uma representação com nomes fictícios, não uma captura de execução.

## Teste no aparelho

1. Em Áudio do clipe, toque em Importar áudio. Autorize o acesso e procure pelo nome de uma gravação ou música, digitando com e sem acento.
2. Escolha um arquivo, toque em pré-escuta e aplique. Revogue a permissão e reabra o projeto para confirmar a cópia privada. Repita no reparo de áudio, conferindo que os cortes permanecem na posição anterior.
3. Negue a permissão e confirme que Procurar arquivos ainda abre Downloads ou o provedor desejado. Para referência de sincronização, verifique também um vídeo.
4. Repita em Música, Faixas, Editor de áudio e Vozes IA. Confirme que a clonagem oferece WAV e preserva a validação de duração. Se disponível, use um aparelho Android 12 ou inferior e um cartão SD indexado.
