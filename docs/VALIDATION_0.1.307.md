# Uai Cortes 0.1.307 — Legendas, nitidez e prévias

Base 0.1.306. versionCode 308. Formato do projeto preservado.

## Diagnóstico do relato

- O log v0.1.306 registra diversas falhas Gemini cerca de 100–200 ms após o início, sem etapa, código HTTP ou motivo. A geração Vosk posterior retornou seis legendas. Não é possível provar retrospectivamente se o Gemini recusou a chave, o envio, a requisição ou outro estágio.
- Há exportação concluída em 720p. O log antigo não mostra as dimensões verificadas do MP4 ou o bitrate obtido. Não permite atribuir perda perceptiva exclusivamente ao encoder.
- A prévia do corte direto gerou MP4 em aproximadamente 10,4 s. O log antigo não diferencia um arquivo pronto de um primeiro quadro exibido.

## Alterações

- Corrigir português envia só o texto do bloco à API de texto do Gemini, por ação explícita. O usuário vê e escolhe as mudanças, que entram no histórico de desfazer. Os horários da legenda ficam intactos. Quando muda a quantidade de palavras, o trecho modificado pode ter novos horários estimados.
- Transcribe usa o identificador e o formato de referência de arquivo do exemplo REST atual (tipo file, file_uri) e lê estado no objeto File. O app mostra etapa e status do erro. Somente uma falha com resultado desconhecido da transcrição gera aviso de possível nova cobrança. Sem reenviar automaticamente falhas de rede; preservado o failover explicitamente ativado para HTTP 429. A ajuda reúne consumo e recuperação.
- Trocar proporção mantém resolução e bitrate escolhidos. O atalho de nitidez seleciona 1080p e pede 28 Mb/s. O dispositivo pode não entregar valores exatos. Dimensões e bitrate medidos entram no diagnóstico, sem caminho, mídia ou título.
- Corte direto reduz a prévia à junção só se puder preservar efeitos e relógio sem trilhas independentes. Outros casos renderizam como antes. Prévia oferece cancelamento e nova tentativa, limite de 45 s para gerar e limite de 9 s em primeiro plano para o primeiro quadro. Ambos os estágios são registrados separadamente.

## Verificação

170 scripts de regressão e a verificação do ZIP extraído, CRC e versão passaram localmente. JUnit adicional cobre classificação, preservação de horários e prévia curta. Gradle e seus testes Android não rodaram neste ambiente. Nenhuma chamada autenticada à API Gemini foi executada.

Em Android, conferir a transcrição Gemini real com um trecho de 10–20 s, um erro HTTP (sem copiar a chave), revisão de português, exportação 9:16 Máxima e prévia PNG → MP4 com e sem uma faixa independente. A [imagem ilustrativa](Previa-Gemini-UaiCortes.png) usa HTTP 429 como exemplo, não como resultado do log do usuário.
