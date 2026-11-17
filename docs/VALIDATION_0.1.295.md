# Validação da 0.1.295

- Ramo de origem: 0.1.294. Identidade: versionCode 296. Schema JSON 4, a mesma assinatura e cinco APKs previstos.
- Verificações de fonte: execute `python3 tools/check_version_sync.py`, `python3 tools/validate_source.py` e os scripts `tools/test_*.py`.
- JVM: `SingleWord295Test` cobre palavras exatas, pausas, dados ausentes e karaokê preservado. `JunctionTransition293Test` cobre a janela sem deslocar a mixagem. `GitHubUpdateCoreTest` cobre as notas recebidas para versões puladas.
- Android: `SubtitlePreviewSync295AndroidTest` decodifica os pixels das palavras e os picos do áudio dentro e fora da pausa, no MP4 temporário e no exportado. O teste deve ser executado em aparelho/emulador. O pipeline padrão compila testes instrumentados, mas não os executa.
- Nenhuma compensação global é aplicada aos tempos do MP4 final. O modo de exibição de uma palavra não subdivide a faixa ao salvar o projeto ou exportar SRT.

## Casos manuais

1. Comparar o loop da junção com o MP4 final usando narração com atraso no início, música em repetição e PiP com áudio. Repetir Original, Ajustar e Preencher. O loop toca até 1,1 s de cada lado, porém o exportador prepara a montagem completa em resolução de prévia para preservar a mixagem.
2. Com uma legenda com horários individuais, conferir palavra curta, pausa, pontuação e busca durante a reprodução. Sem horários individuais, confirmar aviso de estimativa. Comparar karaokê e Palavras acumuladas com um projeto antigo.
3. Guardar um rascunho com nome, abrir os dois projetos e verificar tempos e mídias. A reabertura não deve copiar de novo um arquivo privado ainda legível só porque o ID do projeto mudou. Abrir um projeto com fonte externa indisponível deve conservar a opção de reparo.
4. Conferir as três linhas de ações com fonte grande. Em Atualizações, mostrar notas antes de baixar, pausar, retomar, parar, reiniciar e confirmar que uma mensagem antiga de instalação não aparece em novo download.
5. No diagnóstico, verificar PREVIEW, SUBTITLES, MEDIA e a versão por sessão, sem dados privados. O campo cursor_delta_ms mede a diferença entre o relógio do player e o cursor da interface, não o atraso de decodificação do áudio. Confirmar que Apagar remove apenas o histórico local.
