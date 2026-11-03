# Validação — Uai Cortes v0.1.112 / versionCode 113

## Implementação

- Fonte de referência selecionável: áudio do vídeo do projeto (reprodução dos clipes), faixa adicional existente ou arquivo de áudio/vídeo escolhido pelo SAF.
- Marcação **manual** da posição temporal de cada fala com a pré-escuta local. Para mídia externa, ajustar o início correspondente na timeline.
- Os inícios marcados não substituem os timestamps das legendas nem os arquivos originais; usar os horários somente na revisão e nas faixas geradas depois da confirmação.
- A API Gemini TTS continua recebendo somente o texto das legendas e instruções da voz escolhida, não áudio de referência, e nenhum serviço externo é acionado na etapa de referência.
- Conflitos de sobreposição e excedente do final continuam analisados antes de aplicar no projeto; WAVs válidos não são regenerados por mudanças apenas temporais.

## Validações locais

- `python3 tools/check_version_sync.py`, `python3 tools/validate_source.py`.
- `python3 tools/test_audio_reference_contract.py`, cenário offline puro Kotlin/JVM e integridade do ZIP extraído.
- Análise sintática Kotlin PSI dos arquivos modificados (não equivale à compilação Android).

## Pendências

- Build Android Gradle completo e teste da UI/players com arquivos de vídeo/áudio reais, incluindo múltiplos clipes e fontes sem som.
- Verificar sincronização do MP4 exportado após inclusão das faixas e preservação de URI SAF pós-reinicialização.
- Não há detecção automática de início de fala, transcrição de áudio de referência, replicação vocal nem garantia de duração exata.
