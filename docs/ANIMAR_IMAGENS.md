# Animar Imagens offline · v0.1.50

## Uso

1. Na tela inicial, abra **Animar Imagens**.
2. Escolha fotos na galeria interna, ou **Procurar nos arquivos** para selecionar imagens pelo Android.
3. Reorganize segurando e arrastando a alça de uma foto (ou use as setas). Remova fotos se necessário.
4. Toque em **Animar automaticamente**. A prévia mostra movimentos de zoom/pan entre as fotos.
5. Opcionalmente ajuste intensidade, duração, proporção, qualidade e transições; a taxa padrão é 30 FPS.
6. Toque em **Salvar vídeo MP4**. O processamento acontece totalmente no dispositivo e informa o progresso quando o Media3 disponibilizar.
7. Ao concluir, o vídeo aparece em **Filmes/UaiCortes**. Use **Enviar para timeline do editor** para adicionar áudio, efeitos, PiPs e legendas ao MP4.

## Configurações padrão

- Intensidade normal, 5 segundos/imagem, vertical 9:16, 720 × 1280, H.264 30 FPS;
- Quadros 16:9: 1280 × 720 em 720p; quadrado: 720 × 720.
- Transição automática: fade breve para preto e ocasionalmente branco. **Não é crossfade** entre dois clipes.
- Movimentos determinísticos alternam zoom de entrada/saída e deslocamentos direcionais. A curva de aceleração é smoothstep.

## Limites e segurança

- Até 80 imagens por sequência. Para 1080p, o uso de RAM, armazenamento e temperatura será maior.
- Se uma foto não corresponder ao formato, ela é ajustada sem deformação e sobre um fundo derivado da própria foto. A prévia leve é aproximada e pode divergir do fundo do MP4 gerado.
- Nenhuma imagem é enviada à rede, e não há download de modelos, IA generativa ou API externa.
- A ferramenta **não cria movimento corporal/expressões por IA** e não implementa paralaxe real 2.5D: faltam máscaras e profundidade local confiáveis.
- Os JPEGs intermediários são temporários e removidos depois do êxito, cancelamento ou erro; as mídias originais nunca são alteradas.
- Cancelar interrompe o Media3. Durante a cópia para a galeria, o cancelamento fica desativado para evitar um arquivo parcialmente salvo. Erros de cópia removem a entrada pendente do MediaStore.
- Se a permissão de fotos for revogada, pode ser necessário selecionar as imagens novamente.

## Testes necessários no Android

1. 1, 2 e 12 imagens; retratos, paisagens e quadradas; fotografias com rotação EXIF.
2. Reordenação por arrasto, setas, remoção e escolha adicional no seletor Android.
3. Preview e MP4 de 3/5/8 s, 9:16/16:9/1:1 e 720p/1080p.
4. Progresso e cancelamento durante preparação e renderização; erro de mídia sem permissão.
5. MP4 H.264 aberto na galeria; adição à timeline do editor; áudio/narração adicional.
6. Teste de aquecimento e memória em aparelho modesto.

**Atenção:** validação de código-fonte não comprova exportação real em hardware. Build CI e testes físicos permanecem pendentes.
