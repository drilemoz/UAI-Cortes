## Melhorias v0.1.51

- Prévia reduzida para 238 dp, imagens com cartões compactos; alça ☰ atualiza posição durante o gesto, com rolagem de borda; setas permanecem.
- Botões Ajustes e Animar lado a lado; configurações avançadas compactadas.
- Progresso corrigido para evitar o `NullPointerException` documentado no TXT: capturar o valor local (0–100%) antes de alimentar a lambda Compose; sem `!!`. Sem porcentagem Media3, mostrar barra indeterminada.
- A exibição usa a marca Uai Cortes; o processamento continua no Media3 offline.
- Testar no aparelho: exportar 3 e 15 fotos, reordenar no meio e nas bordas, cancelar exportação, salvar MP4 e enviar à timeline.

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

## Versão 0.1.52: edição padronizada e continuação no editor

1. Importe as fotografias e ajuste a ordem pela alça ou pelas setas.
2. Use **Ajustes** para intensidade, tempo por foto, proporção, qualidade e transição.
3. Toque em **Salvar** (cabeçalho) para guardar **somente o rascunho** da sequência e seus ajustes. Isso não cria vídeo. Ao abrir Animar Imagens outra vez, o rascunho é recuperado se a mídia ainda estiver acessível pelo Android.
4. Pressionar **Voltar** com alterações não salvas abre **Salvar e voltar / Descartar alterações / Continuar**. Se o Uai Cortes estiver terminando a cópia do MP4 na galeria, aguarde a confirmação antes de sair.
5. Toque em **Animar**, avalie a prévia e depois escolha **Salvar MP4**. O app salva o rascunho antes da renderização e a mídia gerada em Filmes/UaiCortes.
6. Na confirmação em tela cheia, escolha **Abrir vídeo**, **Compartilhar** ou **Editar com áudio, legendas e PiP**. Essa última ação leva o MP4 ao **mesmo editor** usado por qualquer vídeo; se a ferramenta foi aberta de dentro de um projeto, o MP4 é acrescentado como clipe, caso contrário vira um novo projeto editável.
7. No editor, adicione áudio, narração, cortes, legendas, marca d’água e PiP, salvando o projeto e exportando novamente depois dessas edições.

**Importante:** o rascunho de fotos e o projeto editável de vídeo são estados distintos. O rascunho referencia URIs locais (MediaStore/SAF); se o usuário revogar permissão ou excluir as imagens, o aplicativo poderá precisar que elas sejam selecionadas novamente. O aplicativo não duplica nem envia fotos para a rede. Esta mudança não implementa áudio no motor de animações antes do MP4, mas reutiliza o editor completo após renderizar.

**Validação pendente:** compilação Gradle/Android, navegação real, persistência após processo reiniciar, permissões MediaStore e exportação/MP4 no celular.
