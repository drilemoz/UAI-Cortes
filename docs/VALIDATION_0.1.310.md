# Validação da 0.1.310 — Legendas em uma linha

- Base 0.1.309, versionCode 311. Schema, assinatura e cinco variantes preservados.
- A prévia do vídeo, os exemplos de estilo e o MP4 passam pelo mesmo rasterizador. A largura é proporcional ao quadro. O desenho reduz a fonte quando a frase não cabe e troca as quebras por espaços sem mudar o comprimento UTF-16, os índices de palavra ou os dados do projeto.
- A prévia de Legendas seleciona no máximo a primeira legenda válida em cada instante, igual ao exportador. As demais continuam no projeto, sem recorte de duração ou exclusão. A marca de texto conserva o desenho e controles legados. O relógio da exportação da 0.1.309 permanece.
- SRT e TXT ficam lado a lado. O seletor de destino, salvamento de SRT em Downloads e conteúdo do arquivo conservam as rotas anteriores. A revisão de português muda apenas o diálogo. Envio textual, revisão, aprovação e nova tentativa continuam explícitos.
- Verificação local: metadados, validador e 175 scripts de regressão. Quatro testes JUnit da política de linha única executados separadamente com o compilador Kotlin. O teste instrumentado compara os pixels do rasterizador e do overlay, verifica a tarja e cobre uma segunda legenda simultânea. Não atesta renderização em aparelho antes de rodar nele.

## Conferência no aparelho

1. Na tela Legendas, inserir um texto longo em um projeto vertical e reimportar um SRT contendo uma quebra. Tocar o trecho e confirmar que cada frase cabe em uma linha na prévia, sem perder palavras.
2. Usar a mesma frase com karaokê rosa, sobrepor duas legendas e comparar um mesmo quadro na prévia e no MP4. Conferir tarja, contorno, posição, frase e horário da fala. Repetir com abertura em foto e PiP.
3. Exportar SRT e TXT para a pasta escolhida e SRT para Downloads. Reimportar o arquivo e conferir que os tempos e as quebras do original continuam lá, ainda que o desenho use uma única linha.
4. Abrir Corrigir português e conferir a ação principal, erro, escolha explícita de outro modelo e aplicação apenas das passagens revisadas.
