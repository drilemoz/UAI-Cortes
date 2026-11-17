# Validação da v0.1.290

- Num projeto com Vídeo, Vozes e Legendas, confira a ordem das pistas. As vazias ficam abaixo, com Foto, PiP, Áudio e Música por último. Tocar num atalho vazio deve abrir o fluxo da própria pista.
- Adicione uma foto ou uma música e confirme que a pista sobe. Remova o último item de uma pista e confira sua volta para o grupo vazio. Arraste e apare clipes sem que a ordem oscile durante o gesto.
- Em projeto só de fotos, verifique os cortes e marcadores de transição na pista principal de fotos. Num projeto com vídeo, mantenha os marcadores na pista de vídeo.
- Em tela baixa, role a área das pistas para alcançar a última linha. Com zoom, role horizontalmente e confira que o atalho das pistas vazias continua acessível.
- Confira que Velocidade abre todas as opções e pode restaurar 1×. Não deve mudar a velocidade do arquivo exportado.
- Os contratos estáticos e testes JVM cobrem a ordenação e os limites de altura. A interação e a renderização ainda devem ser conferidas no Android.
