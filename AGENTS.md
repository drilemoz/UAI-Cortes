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
