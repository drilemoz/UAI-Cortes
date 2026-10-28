# Legendas automáticas offline: configurar e usar (v0.1.49)

1. Abra um projeto com vídeo e entre em **Legendas**.
2. Toque em **Auto** para abrir o assistente.
3. Em **Passo 1**, toque em **Baixar e configurar modelo**. Conecte o aparelho ao Wi-Fi. O app baixa `vosk-model-small-pt-0.3.zip` do site oficial da Vosk (aproximadamente 31 MB), descompacta e guarda em `filesDir/speech-models` privado. Não é preciso instalar APK adicional, e o modelo não acompanha o código-fonte.
4. Confirme a mensagem **Modelo instalado e pronto**. A partir desse ponto, desligar a internet não impede a transcrição.
5. Em **Passo 2**, toque em **Transcrever fala (offline)**. O processo analisa o áudio dos vídeos da linha do tempo e respeita os cortes. O primeiro modo é limitado a projetos de 10 minutos; pode demorar e consumir RAM/bateria.
6. Em **Passo 3**, revise as frases reconhecidas. Toque em uma para corrigir. Toque em **Aplicar legendas ao projeto** para gravar o resultado. Se já existirem legendas, escolha conscientemente se quer adicionar ou substituir.
7. Confira a sincronia na tela Legendas. Edite horários e estilos antes de exportar o MP4.

## Cuidados
- O modelo pequeno é uma opção leve, não um transcritor profissional. Ruído de vento, música, sotaque e múltiplas vozes reduzem a precisão.
- O app não usa o microfone para esse recurso, nem faz upload dos vídeos. A única conexão de rede é o download voluntário do modelo.
- O download só é considerado concluído após verificação dos arquivos do modelo; não instale arquivos recebidos de fontes não confiáveis.
- Se um clipe não tiver trilha de áudio, ele não gerará legendas.
- O motor Vosk e o MediaCodec precisam ser validados em aparelhos Android reais para confirmar funcionamento em cada CPU/codec.

Documentação e download oficial: https://alphacephei.com/vosk/models
Biblioteca Android: https://central.sonatype.com/artifact/com.alphacephei/vosk-android
