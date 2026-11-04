# Legendas — correções e efeitos v0.1.133

## Implementação
- Editar todas oferece três grupos independentes (Estilo, Animação, Posição); cópia adicional de cores individuais é **opcional** e desligada por padrão. Palavras correspondem por texto normalizado e ocorrência; cores de palavras sem correspondência permanecem no destino.
- O texto, os IDs, horários e timestamps por palavra são preservados; a mensagem de confirmação e o desfazer refletem os grupos usados.
- A prévia deixa quadros realmente vazios em efeitos de escrita/revelação. Controles Reproduzir/Pausar/Reiniciar; opção de simulação de 3 segundos ou duração real da legenda.
- Animações novas: `rise`, `drop`, `slide_left`, `slide_right`, `word_reveal`, `elastic`, `float`, `sway`, `soft_shake`. As 10 animações anteriores e os 12 estilos anteriores continuam reconhecidos. 10 novos presets (22 ao todo).
- O deslocamento horizontal `slideX` usa a mesma função temporal no editor, galeria e exportação Media3. Não alterar schema JSON 4 nem os arquivos originais.

## Validação planejada no Android
1. Abrir projeto legado, testar cada efeito em vídeo horizontal e vertical, com áudio e legendas sobre PiP.
2. Conferir quadro inicial vazio dos efeitos de escrita, sem texto substituto.
3. Confirmar Prévia/MP4 no início, meio e fim do efeito, incluindo movimentos horizontais e clipes concatenados.
4. Em Editar todas, testar cópia de cores em palavras repetidas, diferentes e iguais, inclusive alterações de apenas animação ou posição, e desfazer.
5. Verificar desempenho em aparelhos de RAM limitada e acessibilidade/fontes grandes.

**Importante:** sintaxe e testes Kotlin/JVM isolados não comprovam compatibilidade ou fidelidade visual de APK no Android; executar CI e teste físico.
