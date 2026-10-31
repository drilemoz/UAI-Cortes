# Validação 0.1.67 / versionCode 68

## Mudanças funcionais

- Acesso direto à exclusão individual e confirmação da exclusão atômica de todos os projetos, sem apagar arquivos de mídia.
- Nome editável antes da seleção de vídeos, persistido em `VideoProject.title` e sugerido no nome do MP4, sanitizado sem mudar o schema JSON.
- Galeria com pesquisa opcional e sem chip de tipo redundante para seletores de tipo único.
- Instalador Vosk separado das legendas, progresso numérico de transferência, mensagem de etapa, cancelamento e validação do arquivo recebido. URL oficial mantida.
- Faixas por clipe para posicionar temporalmente PiP e campos compactos; vídeo PiP não é estendido além da duração da fonte.

## Evidência local

- `python tools/validate_source.py`: aprovado após sincronizar requisitos da interface.
- Parser PSI Kotlin: 134 arquivos Kotlin, 0 erros sintáticos.
- Regras puras `ProjectNaming` e `PipTimePresets`: compiladas em Kotlin/JVM e smoke checks executados, incluindo nomes inválidos, clipes consecutivos e limites da duração da mídia.
- ZIP: integridade CRC verificada na entrega.

## Limites

- Compilação Android/Gradle integral não disponível neste ambiente: SDK Android e distribuição Gradle ausentes.
- JUnit do Gradle, visual em dispositivo, exportação H.264 e instalação real do Vosk continuam pendentes.
- Sem teste de rede real no aparelho, não afirmar que o download anterior foi consertado; mensagens de erro e progresso foram melhorados para diagnóstico.
- A duração de um PiP de vídeo maior que sua mídia não é implementada com loop; a predefinição usa no máximo a duração da mídia validada.
