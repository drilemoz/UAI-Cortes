# Uai Cortes 0.1.305 — Modelos e encaixe de voz

Base 0.1.304, versionCode 306. Formato do projeto preservado.

- O limite de cinco se aplica exclusivamente à galeria de modelos de fábrica. A lista de legendas volta a rolar inteira. Modelos pessoais e estilos de projetos salvos ficam disponíveis e não são migrados.
- Nova fica na linha superior de ações. Automáticas entra na faixa de ferramentas. O ícone de Personagens abre a definição de personagens e os filtros, inclusive Todas.
- Karaokê rosa usa tipografia pesada, padrão de 20 sp, contorno preto e tarja mais folgada apenas na palavra ativa. O exportador reserva espaço para a tarja na borda. A edição dos horários por palavra preserva o karaokê selecionado.
- O encaixe usa início/fim da legenda, o próximo início e o fim do projeto. Para fala curta, tenta remover silêncio das pontas e acelerar por WSOLA até 1,25× quando necessário. Imports comprimidos dependem da decodificação PCM do Android. Arquivos grandes, diferenças excessivas ou formatos que não puderem ser analisados não são forçados a caber. Sem alterar vídeo, texto ou tempos de legenda e sem requisição adicional ao Gemini. O recurso não compara fonemas com os lábios do vídeo.
- Voz externa importada passa a usar também o teto de 10% no volume original dos clipes atingidos. A fala já importada tem opção manual de executar o encaixe em Vozes IA.
- A contagem de inserção ignora o mesmo arquivo já instalado para a mesma fala e versão do texto. Gerar um arquivo novo permite substituir. A gravação continua explícita.

168 scripts de regressão passaram, além da validação geral, CI70 e CI72 estáticos e sincronização de versões. Testes JUnit incluem conservação do WAV, limite na próxima fala e detecção da mesma versão instalada. A compilação e o lint não rodaram pela ausência de Gradle e do wrapper JAR.

Em Android, verificar geração de cinco falas e queda da contagem após inserir, uma regeneração, importação de WAV e MP3 com silêncio, diferença de duração acima de 25%, contraste e margem do rosa na prévia e no MP4. A imagem [Prévia](Previa-Modelos-UaiCortes.png) é ilustrativa.
