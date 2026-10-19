# Regras de manutenção do Uai Cortes
- Atualizar sequencialmente `versionName`, `versionCode`, README, RELEASE_NOTES e nomes do APK em workflow.
- Entregar o APK diretamente em Release Assets no formato `UaiCortes-vX.Y.Z.apk`.
- Entregas de código-fonte podem ser ZIP, mas a saída de instalação não pode ser ZIP.
- Sem autenticação, IA, publicidade ou serviço de nuvem para edição.
- Não declarar recurso pronto sem implementação e teste. Fade/dissolvência continuam pendentes.
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
