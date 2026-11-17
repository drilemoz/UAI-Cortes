# Validação 0.1.293 — Transições de vídeo

## Ensaios automatizados

- `tools/test_junction_transitions_293_contract.py` verifica o caminho do painel e do arquivo de prévia pelo exportador, a faixa de apoio sem áudio, a ordem das camadas, a preservação da duração e os pontos de limpeza.
- `JunctionTransition293Test` verifica sobras da fonte, quadros de apoio, as curvas na junção e o mapeamento temporal da prévia.
- `JunctionTransition293AndroidTest` verifica MP4 decodificado com dois vídeos sintéticos em 9:16. Confere mistura de vermelho e azul, ambas as direções de empurrão, duração e presença de áudio. Os clipes de teste são cores sólidas e tons de 440 Hz e 740 Hz, gerados localmente.
- Android e Media3 precisam ser validados em runtime. As verificações de código não decodificam vídeo.

## Ensaio de aparelho

1. Abra uma montagem 9:16 com duas cenas com fala, legenda próxima à junção, música e PiP. Anote a duração. Toque no marcador entre os clipes.
2. Veja Dissolver em loop, alterne o som dos clipes e confira os dois sentidos de Empurrão. Mude de corte sem aplicar e confirme que o rascunho não foi salvo.
3. Aplique numa única junção e exporte em Original e em Vertical 9:16. Compare os quadros da junção, a ordem de legenda/PiP, o áudio e a duração original. Avalie 720p/1080p, duas junções consecutivas, projeto de várias cenas e mídia com fontes de proporções e FPS diferentes.
4. Repita com clipes inteiros e depois com sobra em ambos os lados. A primeira configuração usa quadros de apoio. A segunda usa mídia fora do recorte. Verifique em dispositivo com poucos decodificadores simultâneos e com clipes HDR, cuja saída combinada é SDR.
5. Cancele uma prévia ainda renderizando e tente novamente. O MP4 de prévia não deve aparecer na galeria. Se a composição falhar no aparelho, não substituir silenciosamente o efeito.

A prévia do painel usa a saída Original a 360p e o som dos clipes. Faixas adicionadas de música e narração não são reproduzidas nesse loop curto. A reprodução contínua do editor ainda é sequencial. O painel renderizado e o MP4 são onde se vê a transição combinada.
