# Validação — Uai Cortes v0.1.199 (versionCode 200)

## Escopo
- `UaiShapes` centraliza formas Material 3 e os contornos leves de botões, cartões, diálogos e sheets. Adaptação visual inicial em Home, PiP, Áudio, salvamento, Vozes IA, Legendas, Novidades e Narração.
- PiP: prévia começa com altura máxima de 250 dp, expansível; ferramentas reduzidas a 58 dp, ajuda recolhível e controles de transparência compactados. A edição gestual, a posição fracionária, os horários, os ajustes e a composição MP4 seguem inalterados.
- Áudio: player Media3 isolado para pré-escuta de cada faixa importada e música, respeitando volume e intervalos, pausando os demais players e liberando recursos ao sair. A extração do projeto continua acessível no painel recolhido; o atalho rápido da Home permanece.
- Diálogo de saída: três ações horizontais com botões de altura 64 dp e cantos de 8 dp; salvamento e confirmação permanecem iguais.

## Testes locais
- Contratos `tools/test_visual_pip_audio_199_contract.py`, `tools/test_audio_gemini_flow_contract.py`, `tools/test_editor_polish_175_contract.py`, `tools/test_ci161_project_draft_height_import.py`, `tools/test_ci163_media3_quick_audio_optin.py`, `tools/test_quick_video_gallery_198_contract.py`, `tools/test_version_sync.py`.
- Sintaxe Kotlin PSI dos 11 arquivos relevantes e versionamento `tools/check_version_sync.py`.
- Validação estrutural de fonte e CRC do pacote, quando concluída.

## Pendente no Android/CI
- Build completo `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease`.
- Verificar a pré-escuta de arquivos WAV/M4A longos, recortes, erro de leitura e alternância de mixagem com faixas importadas.
- Testar PiP vertical e horizontal, redimensionamento, precisão de gestos, fontes ampliadas, tela pequena, modo AMOLED e prévia/exportação equivalentes.
- Confirmar UI de saída e diálogos com fonte ampliada no dispositivo. Não confundir contratos/PSI com build Android real.
