# Validação Uai Cortes v0.1.109

## Alterações

- O vídeo de referência enviado pelo usuário mostra que `Confirmar voz` fechava os diálogos e retornava ao editor sem geração. A nova lógica fecha apenas o seletor; o botão `Gerar e usar` exige confirmação da cota e solicita um WAV.
- Após resposta WAV válida e arquivo persistido, a callback grava o novo `AudioTrack` no projeto e mantém a tela Áudio aberta. A confirmação também cobre a inclusão. O botão `Adicionar à mixagem` permite recuperar uma geração já salva sem repetir a chamada se a persistência do projeto falhou.
- `Ouvir mixagem` reproduz clipes (som original) e faixas adicionais sincronizadas pela posição global, somente na prévia. O botão `Volume da prévia` no editor limita todos os players de vídeo, música, faixas e PiP, sem alterar dados salvos/exportados.
- Removido somente o GET de validação de modelo antes da chamada `interactions`; isto não muda o endpoint de síntese nem implica que a conta tenha acesso ao modelo.

## Verificações locais

- `python3 tools/validate_source.py`
- `python3 tools/test_version_sync.py`
- `python3 tools/test_audio_gemini_flow_contract.py`
- `python3 tools/test_voice_picker_contract.py`
- `python3 tools/test_voice_return_contract.py`
- `python3 tools/test_editor_preview_volume_contract.py`
- Kotlin PSI dos seis arquivos Kotlin principais e teste JVM de política de volume.
- Verificação e extração do ZIP completo por `tools/package_source.py`.

## Ainda pendente

- Compilação completa Gradle/Android e Android Lint no GitHub Actions.
- Teste real com Gemini (credenciais/cota/modelo), geração e inclusão na timeline, reprodução de faixas simultâneas, volumes temporários, PiP e áudio final do MP4 em aparelho.
