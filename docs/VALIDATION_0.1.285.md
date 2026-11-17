# Validação da v0.1.285

- Em um projeto com MP3 em Áudio, confirme que a faixa Música aparece vazia. Mova o MP3 e confira que só muda de faixa, com o mesmo início, volume e trecho. Confira Desfazer e Refazer.
- Com uma música já instalada, teste cancelar e confirmar a substituição. Narração e Vozes IA devem permanecer. Salve/reabra e confirme o recorte da música, inclusive após ajustar só o volume.
- Confira as alturas menores, o nome da legenda ao tocar e o marcador de transição. Os gestos de aparar e arrastar dos clipes continuam disponíveis.
- Selecione um último clipe com sobra de fonte. A régua só exibe espaço extra ao tocar em Estender final. Puxe a borda direita e confira que a régua volta a terminar no projeto. Verifique também com zoom.
- O teste JVM cobre transferência, preservação do recorte, fonte ausente, substituição e alturas. O teste instrumentado MusicExportTimingAndroidTest parte de uma fonte com silêncio inicial para confirmar que o MP4 usa o trecho recortado. A conferência visual e sonora do MP4 exige Android.
