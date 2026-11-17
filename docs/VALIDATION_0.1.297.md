# Validação da 0.1.297

- Build 253 (0.1.296): 735 testes JVM tentados, quatro falhas em contratos de contagem de animações/presets de legenda. Asserções corrigidas para conferir a presença dos modos esperados, bem como a preservação de texto e timing. O modo novo continua ativo.
- As transições têm quatro escolhas. Efeitos que já estão salvos carregam e exportam, mas não se propagam em lote sem escolher uma opção oferecida.
- No MP4 de 26,48 s enviado com o relato, ambos os streams começam em 0, e suas durações diferem em cerca de 14 ms. Há cortes visíveis por volta de 5,00 s, 10,10 s, 15,23 s e 20,37 s. Movimentos da boca também aparecem em pausas dentro da cena do réu. A leitura do arquivo renderizado não identifica se esse movimento veio da fonte, da dublagem ou da exportação.
- As composições novas impedem que o lado de apoio de uma fonte com áudio mostre movimento fora de seu intervalo. Validar no Android se os quadros de apoio não introduzem visuais estranhos, se as quatro opções funcionam e se a fala, depois do corte, continua no horário original.
- Testar Aplicar a todos ao lado do som com um projeto de três clipes, desfazer ou cancelar sem gravar, reabrir uma junção antiga e exportar o projeto tendo Corte direto, Dissolver e vídeo vertical com voz.
- Compilação e testes Kotlin/Android não são substituídos pelos contratos estáticos locais.
