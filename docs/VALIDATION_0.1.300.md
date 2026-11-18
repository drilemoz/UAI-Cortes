# Uai Cortes 0.1.300 — Entrada de logo e fotos PiP

Base 0.1.299, versionCode 301. Preservados assinatura, schema 4 e cinco variantes do APK.

## Evidência

No MP4 enviado, o plano principal é substituído por um recorte branco/azul com a aparência do logo durante dois quadros, em 0,481 s e 0,514 s. A imagem normal com o logo no canto reaparece em 0,548 s. A primeira troca de clipe só ocorre aproximadamente aos 5 s. A coincidência localiza o gatilho na entrada da imagem, mas não demonstra a causa exata no compositor ou codec do aparelho.

## Prevenção

O bloco consecutivo de fotos PiP no topo da pilha deixa de ser sequência de vídeo. As fotos são decodificadas localmente uma vez e desenhadas com a geometria normal sobre o quadro da composição. Antes do início e após o fim, a textura é transparente e o alfa é zero. A opacidade da janela, posição, ajuste e ordem do editor são preservados, com legendas e marca de texto acima. Se uma foto estiver abaixo de um PiP de vídeo, ela permanece no compositor original para não alterar a ordem.

O teste é uma exportação nova e não repara o arquivo antigo. O fluxo de áudio, a ordem de quadros da fonte, o diagnóstico de engasgos e os efeitos de transição não mudaram.

## Testes

- `ImagePipExportPlanTest` cobre o primeiro e último instante, além da ordem de fotos e vídeos.
- `ImagePipFlashAndroidTest` gera um MP4 privado com logo de teste entrando em 0,5 s. Inspeciona os pixels do centro, da área do logo e de fora dela antes, durante e após a entrada. Requer Media3, OpenGL e codificador AVC do Android.
- Em aparelho, reexporte o projeto com o mesmo logo, sem alterar seus horários. Confirme quadro a quadro que não há troca integral da tela em torno de 0,5 s.

167 verificações estáticas passaram. Gradle Android não está instalado neste ambiente, portanto o APK e o teste de pixels no Android não foram executados.
