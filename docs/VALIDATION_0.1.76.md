# Uai Cortes 0.1.76 / versionCode 77 — Implementação Gemini TTS

## Base e arquitetura inspecionadas

- ZIP original: UaiCortes-v0.1.75-Audio-do-Clipe.zip, com versionName 0.1.75 e versionCode 76.
- Regras: AGENTS.md (esquema de projetos 4, cinco APKs Release, assinatura via Secrets, EditorProjectViewModel/ProjectStore, áudio via AudioTrack e ClipAudioBindings, Media3).
- Configurações: HomeScreens.SettingsScreen, sem criar rota extra.
- Editor de áudio: AudioScreen/AudioSectionUi, extração e seletor SAF; volume, recorte, preview.
- Projetos: AudioTrack persistido em VideoProject.json(), ProjectStore, schemaVersion 4.
- Linha do tempo e prévia: EditorTimelineUi e TrackAudioPreview em EditorScreen.
- Exportação: ExportEngine cria EditedMediaItemSequence de audioTracks, Gains, lacunas e clipping pelo Media3. Nenhum mecanismo de MP4 ou assinatura foi substituído.

## Implementação

1. GeminiCredentialStore: Android Keystore AES-256-GCM com IV por gravação e preferências privadas, sem chave hardcoded. Backup do app permanece desabilitado e XMLs excluem explicitamente credenciais e áudios de restauração. Invalidação remove blob e tenta apagar alias.
2. GeminiKeyParser: TXT simples, .env com GEMINI_API_KEY/GOOGLE_API_KEY, JSON plano; validação e pré-visualização mascarada. Interface integra importar/exportar via SAF, colar, copiar sensível, mostrar/ocultar, salvar, testar, remover e acesso ao AI Studio.
3. GeminiTtsClient: modelos Flash TTS e Flash-Lite TTS 3.8; verificação de modelo GET; consulta real de vozes via GET /v1beta/voices, inclusive fallback do catálogo geral; POST /v1beta/interactions com speech_metadata.style, speech_config.voice, response_format audio/wav. Sem retentativa automática.
4. GeminiWaveParser: MIME, RIFF/WAVE, chunks, PCM mono/16-bit, bytes e duração; não embrulha WAV novamente. Limite de resposta para reduzir OOM.
5. GeminiNarrationSection: texto, contador, modelos, vozes, estilos estruturados, confirmação de consumo de cota, prévia sintetizada, progresso indeterminado real, cancelamento de corrotina e conexão, reprodução, gravação via SAF e adição ao AudioTrack na posição atual da timeline.
6. GeminiAudioFiles: arquivo WAV privado com gravação em .part + fsync + renomeação atômica, espaço livre e limpeza do .part; URI interna durável persistida no projeto.
7. As trilhas permanecem editáveis pelos controles de recorte/volume/movimentação existentes e são incluídas na composição MP4 existente. Projetos antigos e recursos offline preservados.

## Validações executadas

- `python3 tools/validate_source.py`: aprovado, incluindo as regressões existentes e novos checks de Gemini.
- Parser PSI Kotlin via KotlinCoreEnvironment: **165 arquivos Kotlin, zero erros sintáticos** na validação final. Não substitui a compilação do Android Gradle Plugin.
- Execução Kotlin/JVM isolada de GeminiWaveParser, GeminiModels e GeminiApiError: 6 cenários básicos de PCM 24 kHz, arquivo truncado, cabeçalho RIFF duplicado, whitelist de TTS e HTTP 400/429 aprovados.
- XML/JSON: parse da estrutura final aprovado (**6 arquivos XML**; não havia arquivos JSON estáticos no ZIP de código-fonte).
- Testes JUnit criados para TXT/ENV/JSON, modelos, WAV, HTTP e persistência de AudioTrack. Testes Android instrumentados para Keystore/armazenamento criados; **não executados**.

## Limitações e pendências

- Não existe Gradle nem distribuição instalada no ambiente: `sh gradlew --version` retorna “Gradle não está instalado”. Não foi executado `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease` nem os testes instrumentados.
- Não foi usada chave pessoal real, portanto não houve solicitação live nem confirmação de preços, cotas, modelos ou voz desta conta.
- Media3 declara suporte de entrada WAV, mas exportação com WAV mais vídeo, fade/transição e reprodução em aparelho físico ainda precisam ser confirmadas. Não declarar APK funcional/certificado nesta etapa.
- Controles do projeto para fade de transição de vídeo são existentes; não foi criado efeito novo de fade por faixa de narração nem prometida equivalência não testada.
- Novas narrações ficam no armazenamento privado; excluir o projeto não apaga automaticamente os WAVs. Por segurança, não foi implementada remoção automática que poderia apagar arquivos referenciados por outros projetos.
- Uma geração cancelada pode já ter consumido cota no servidor, mesmo que sua resposta não seja usada.
- Android Keystore protege segredos em repouso, mas não elimina riscos de aparelho comprometido, clipboard ou exportação plaintext. Para distribuir chaves do desenvolvedor use backend, nunca aplicativo cliente.

## Testes necessários no CI e aparelho

1. `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` com Java 17/Android 36/Gradle 8.11.1.
2. `:app:connectedDebugAndroidTest` com Android Keystore disponível, sem expor chave real.
3. Testar importação e exportação SAF com .env/.json/.txt, alterar/restaurar chaves, clipboard sensível e rotação.
4. Testar GET modelos/vozes e gerar amostra com chave própria e cotas conhecidas (observando erro 400/401/403/404/429/5xx).
5. Reproduzir WAV, usar timeline, volume e corte, salvar e reabrir projeto, compor MP4 com narração e verificar A/V sync/fades no dispositivo.
6. Verificar os cinco APKs Release e assinatura, sem duplicá-los em Artifacts.
