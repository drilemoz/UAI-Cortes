# Regras de manutenção do Uai Cortes
- Atualizar sequencialmente `versionName`, `versionCode`, README, RELEASE_NOTES e nomes do APK em workflow.
- Entregar cinco APKs Release diretamente em Release Assets no formato `UaiCortes-vX.Y.Z-{arm32,arm64,x86,x86_64,universal}.apk`; nunca publicar APK duplicado como Actions Artifact.
- Entregas de código-fonte podem ser ZIP, mas a saída de instalação não pode ser ZIP.
- Sem autenticação, IA, publicidade ou serviço de nuvem para edição.
- Não declarar recurso pronto sem implementação e teste. Dissolvência real continua pendente; fade para preto/branco precisa de compilação e teste em aparelho.
- Nunca adicionar chaves, tokens ou keystores ao repositório.

- PiP: manter aspecto original ao carregar logotipos, sem crop, controles Começa em/Termina em e nove atalhos de posição, agora com quatro controles quadrados compactos em uma linha e painel inferior ModalBottomSheet por cima dos controles (nunca no centro ou expandindo a página).
- PiP: manter lista de até quatro camadas e reposicionamento automático por espaço e intervalo de tempo; evitar alterações silenciosas se não couber.
- PiP: manter biblioteca por imagens/vídeos e pastas, alternativa SAF, controle de áudio de vídeo separado e opção silenciosa persistida.
- Confirmar fidelidade entre visualização e exportação no aparelho físico antes de afirmar funcionamento completo.

- PiP v0.1.10: Bottom Sheet sem alça superior, 4 ferramentas na mesma linha, edição direta dos horários, PiPs sequenciais (começar depois do último) e diálogo quando acabar o vídeo.
- PiP v0.1.10: colisões detectadas por região e período; permitir sobrepor somente mediante aprovação explícita do usuário para o par afetado, persistindo `overlapWith` no JSON, preservando auto-reposicionamento como alternativa.

- v0.1.11: tela Cortar e unir com preview do vídeo, miniaturas locais, RangeSlider duplo, cursor de reprodução, marcação início/fim, aplicação explícita, opção desfazer, divisão, reordenar e excluir.
- v0.1.11: entrada de tempo compartilhada `NumericTime` usa dígitos em décimos da direita para esquerda; máscara sem separadores no valor, validação do intervalo antes de salvar; reutilizar nos horários da tela PiP.

- v0.1.12: Cortar e unir deve manter miniaturas tocáveis abaixo do player, sem RangeSlider sobre elas; campo Tempo `autoApply` move o cursor imediatamente após entrada válida e os campos início/fim continuam locais até Aplicar corte.
- v0.1.12: botões Dividir, Desfazer, Antes, Depois e Excluir em uma única linha; marque fim deve explicar quando o cursor não produz intervalo mínimo, e Desfazer cobre a última alteração de lista de clipes.

- v0.1.13: reproduzir indefinidamente na prévia de corte (REPEAT_MODE_ONE) e editor (REPEAT_MODE_ALL), até pausar.
- v0.1.13: seletor 0,25×–2× só afeta prévia; sincronizar PiPs com velocidade do editor. Nunca afetar o MP4 exportado sem escolha separada do usuário.
- v0.1.13: mini tutorial textual abaixo dos botões Cortar/Dividir/Desfazer/Mover/Excluir, preservando organização compacta e suporte a temas.

- v0.1.14: mesma geometria e barras retangulares nas faixas Vídeo/PiP/Áudio/Legenda; toque em clipe abre Bottom Sheet com volume individual, duplicar, cortar, mover e excluir com confirmação.
- v0.1.14: schemaVersion 4, legendas manuais e SRT por tempo absoluto, fades por par de IDs, faixas de áudio extras, extrair M4A/AAC com SAF, sem inventar suporte a MP3/WAV ou dissolvência.
- Conservar compatibilidade de projetos legados, controle de volume na prévia/exportação, 4 PiPs e progressão sequencial.

- v0.1.15: a API `ModalBottomSheet`/`rememberModalBottomSheetState` exige opt-in Kotlin Material3 nos respectivos composables; preservar o opt-in AndroidX do Media3 separadamente. Capturar `maxWidth` diretamente no `BoxWithConstraints` antes de entrar em `Row` aninhado. Não confundir análise sintática com build Android.

- v0.1.16: manter `splits.abi` nas quatro ABIs e `isUniversalApk=true`, todas com o mesmo versionCode para downloads externos (não configura multi-APK Play Store). Empacotar somente metadados Release via `tools/package_apks.py`; abortar CI se não houver exatamente cinco saídas válidas.
- v0.1.16: não adicionar gatilho `push.tags: v*` ao workflow de Releases automáticas; criação de tag pelo `gh release create` provoca segundo build. Não adicionar `actions/upload-artifact` com cópia dos APKs já publicados.
- v0.1.16: chamadas `UnstableApi` da Media3 devem permanecer isoladas em função Kotlin anotada com `androidx.annotation.OptIn`, não dentro de lambdas `AndroidView` de Compose; validar lint no CI.

- v0.1.23: manter `AudioTrack.sourceStartMs` (entrada no arquivo) distinto de `startMs` (posição no projeto); `durationMs` é a duração do trecho após o recorte. Projetos antigos assumem início de fonte 0 ms.
- Ajustes de faixa precisam refletir na prévia do editor e no `ExportEngine`, sem modificar arquivos originais. Não afirmar teste de áudio real sem Android.

- v0.1.24: edição visual de áudio deve modificar campos `sourceStartMs`, `startMs`, `durationMs` apenas por ações explícitas; alças de trim no editor de Áudio são rascunho até Salvar corte, enquanto o arrasto da timeline principal grava a posição ao soltar.
- Gestos não podem mover/clipe para fora do projeto ou da mídia de origem, nem produzir trecho <250 ms. A faixa de música legada continua abrindo a tela Áudio, sem fingir recorte suportado.

- v0.1.25: legendas usam posição normalizada em x/y, com âncora horizontal central e vertical inferior igual ao exportador. Não restaurar offsets fixos em dp; verificar formatos de vídeo distintos antes de afirmar fidelidade completa.
- O arrasto da legenda só persiste no projeto ao soltar; a miniatura é recurso local opcional. Passos de ajuste fino do cursor mantêm limites [0,duração].

- v0.1.26: edição de legendas mantém IDs e intervalos absolutos; cada bloco da timeline deve selecionar o texto correto. Gestos modificam só `startMs`/`endMs`, salvando ao soltar, sem alterar o arquivo SRT ou reescrever propriedades visuais. Não bloquear legendas legadas curtas; manter campos numéricos de tempo como alternativa.
- v0.1.27: `SubtitleCue.backgroundColor` e `bold` devem manter defaults compatíveis com projetos legados; estilos são aplicados à prévia e à exportação, com cor ARGB. Fundo transparente não recebe BackgroundColorSpan. Copiar aparência para todas nunca modifica IDs, texto, início/fim ou posição X/Y. Não aumentar schemaVersion sem necessidade de migração.

- v0.1.28: guardar apenas IDs de famílias de fontes locais (`sans`, `serif`, `mono`) em `SubtitleCue` e normalizar desconhecidas para `sans`; contorno limitado a 0–4 dp (0 = desabilitado). Projetos antigos assumem fonte sans-serif e nenhum contorno. Não mexer na posição ou tempos ao aplicar estilos e não alterar schemaVersion 4. Conferir fidelidade de fontes/contornos e alinhamento multilinha no MP4 real antes de declarar equivalência completa.

- v0.1.29: `SubtitleCue.shadowOffsetDp` (0–6), `shadowColor` (preto/branco), `lineSpacingPercent` (100–160) e `textAlignment` (`left`/`center`/`right`) são atributos opcionais por legenda; defaults legado 0, preto, 100, centro. Contorno/fonte e schemaVersion 4 preservados.
- Sombra implementada como cópia de texto deslocada, sem blur; espaçamento e alinhamento afetam linhas dentro do bloco, não posição X/Y. Conferir Compose versus Media3 no Android antes de afirmar fidelidade visual total.

- v0.1.30: no Compose, o parâmetro `Stroke.join` usa `androidx.compose.ui.graphics.StrokeJoin`; `PathJoin` não existe nesta API. Não reintroduzir o import ou uso antigo. O workflow 26 falhou em Debug/Release por esse motivo. Atualizar sempre Gradle, workflow, `tools/package_apks.py`, validador e docs ao avançar de versão.

- v0.1.31: o cartão Salvar no editor chama a persistência de projeto antes de voltar à Home, e a barra inferior é rolável circularmente nos dois sentidos. Não confundir salvar projeto com exportar MP4.
- v0.1.31: na tela Cortar e unir, o novo Salvar permanece após Excluir em faixa horizontal; deve validar e aplicar o corte pendente antes de voltar à Home, mantendo o usuário na tela se o intervalo for inválido.

- v0.1.32: ao dividir, criar parte esquerda e direita na ordem original, selecionar a direita e posicionar o cursor na fronteira usando horário de origem. Persistir IDs/horários opcionais do último corte sem mudar `schemaVersion=4`.
- v0.1.32: faixa LazyRow mostra miniatura de cada parte visível (carregamento sob demanda), duração e seleção; rolar automaticamente para o trecho escolhido. Desfazer e excluir devem manter marcador consistente, sem afetar arquivos originais nem transições de clipes vizinhos.

- v0.1.33: Legendas tem uma única timeline horizontal virtualizada e lista LazyColumn compacta. Não reintroduzir uma pista completa por legenda nem duplicar lista. Prévia Media3 fica acima da lista, não acompanha rolagem; apenas o editor da legenda ativa pode expandir.
- v0.1.33: edição avançada inicia recolhida; preservar campos numéricos de tempo, gestos para ajustar intervalo, arrasto x/y, presets/fonte/contorno/sombra e `schemaVersion=4`. Ao escolher uma legenda, reproduzir prévia a partir do início; prévia não altera exportação. Validar alinhamento UI/export em aparelho.

- v0.1.34: filtro textual de legendas deve ser insensível a acentos/maiúsculas, manter a numeração original e não alterar projeto enquanto se busca. Navegação cronológica anterior/próxima não deve fazer loop entre extremos; a prévia deve buscar o início da legenda ativa.
- v0.1.34: exclusão requer confirmação explícita e não pode remover o arquivo de vídeo. Criar legenda apenas se houver duração positiva, com horários dentro de [0, duração]. SRT maior que 250 mil caracteres deve falhar visivelmente, sem importar conteúdo truncado. Manter `schemaVersion=4`.

- v0.1.35: toda prévia de vídeo deve preservar a proporção do arquivo e, em projetos mistos, acompanhar o clipe ativo. Prévia inline usa altura responsiva limitada pela tela, preservando lista/controles abaixo.
- v0.1.35: legenda na prévia adapta tamanho apenas quando a maior palavra não cabe na largura do vídeo; não modificar `SubtitleCue.sizeSp`, posição, tempos, SRT ou exportador de forma implícita. Confirmar alinhamento visual e MP4 com teste físico antes de declarar fidelidade total.

- v0.1.36: `searchQuery` deve ser declarado antes de `rememberLauncherForActivityResult` na tela `SubtitleScreen`, pois o callback de SRT limpa a pesquisa; o workflow 31 falhou em Debug/Release quando a variável estava declarada depois. Preservar teste estrutural de ordem no validador, mas sempre confirmar build Android no CI.

- v0.1.37: `SubtitleCue.sizeSp` padrão de novas legendas = 12; ao ler projeto anterior sem `sizeSp`, assumir 22. `animation` e `animationSpeed` opcionais com defaults sem efeito / 1.0. Não elevar schemaVersion=4.
- v0.1.37: editor modal usa rascunho; Cancelar não persiste edições. Aplicar a todas sempre confirma e copia somente aparência, posição e/ou animação conforme seleção; IDs, texto e tempo ficam intactos. Preview e Media3 usam o mesmo SubtitleAnimations.frame, mas fidelidade de renderização física depende de teste Android.

- v0.1.38: Salvar em Legendas persiste projeto editável e volta à Home; preservar Nova/SRT e busca na mesma área. Efeitos devem ficar em LazyRow circular sem duplicar dados persistidos.
- v0.1.38: Áudio mantém volumes legados acessíveis por expansão e faixas adicionais acessíveis sem expandir, com volume individual opcional; remover faixa precisa de confirmação e não apaga arquivo original. Projetos antigos continuam sem migração (`schemaVersion=4`).

- v0.1.39: `EditedMediaItem.Builder.setDurationUs` para vídeo/áudio NÃO deve receber a duração do recorte. Para `Transformer` com duração intrínseca, deixe Media3 ler o arquivo original. Mantenha clipping explícito e valide acesso às mídias e os limites dos cortes.
- v0.1.39: prévia de Exportar desenha legendas/PiPs/fades salvos, mas a exportação real precisa ser testada em aparelho com áudio e vídeo. Apresentar código/causa original em erros de AssetLoader, não afirmar que foi resolvido sem novo teste.

- v0.1.40: `TextOverlay.getBitmap` não aceita texto vazio, pois gera StaticLayout de largura zero. `TimedSubtitleOverlay.getText` deve sempre fornecer placeholder NBSP transparente nos intervalos sem legenda ou na primeira amostra do typewriter, e `getOverlaySettings` precisa usar alpha 0 nessas condições. Guardar texto/tempos/SRT originais sem alterações; testar exportação física antes de declarar resolução definitiva.

- v0.1.41: diagnóstico desativado por padrão; registros locais de tela/toque aproximado (5%) e erros categorizados, sem texto/URI/nome de mídia/projeto/tokens. Histórico rotativo ≤512 KiB, apagável; compartilhamento apenas iniciado pelo usuário. Não gravar teclas ou capturar tela e não adicionar upload automático.
- v0.1.41: usar FileProvider limitado ao cache de diagnósticos, respeitar callbacks de erros Media3/transformer e manter compatibilidade de projetos (`schemaVersion=4`). Confirmar taps, crashes, exportação e permissões no aparelho após compilar.

- v0.1.42: legenda na exportação deve fazer StaticLayout no máximo 90% da largura REAL do quadro, com fonte escalada pela largura e quebra automática. Não regressar ao TextOverlay padrão de largura baseada em linha única. Teste em MP4 físico pendente.
- v0.1.42: mostrar confirmação de sucesso em tela cheia SOMENTE depois da gravação concluída no MediaStore. Deve oferecer abrir, compartilhar, fechar e voltar ao editor; não exibir sucesso se falhar.

- v0.1.43: antes da confirmação em tela cheia da exportação, declarar `savedName` e `savedVideo: Uri?` como estado de `ExportScreen` ANTES de callbacks `onSuccess` do Media3. CI workflow 37 falhou por referências não resolvidas; regressão estrutural no validador não substitui compilação real.
