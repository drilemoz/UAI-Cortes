# Uai Cortes 0.1.15

Editor offline Android nativo em Kotlin, Jetpack Compose e Media3.

## Versões e ferramentas
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`.
- `versionName = 0.1.15`, `versionCode = 16`.
- Kotlin 2.1.21, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- Media3 1.10.1, ExoPlayer e Transformer para composição H.264/AAC.
- O editor não exige conta nem internet. A biblioteca PiP solicita acesso às fotos/vídeos do Android, integral ou parcial conforme consentimento; o seletor de documentos continua como alternativa.

## Telas
- Início, Projetos, Editor, Cortar/Unir, PiP, Áudio, Transições (corte direto, fade preto e fade branco), Exportar MP4.
- Configurações, Geral, Tela e Sobre são **telas**, não pop-ups.
- Ícone azul integrado a `mipmap-*`; original também em `design/icone-uai-cortes.png`.

## Ajustes herdados da versão 0.1.10
- Quatro ferramentas na mesma linha, painel inferior sem alça, com margens compactas.
- Toque nos horários (prévia/editor, começo e fim de cada PiP) para informar **mm:ss**, **mm:ss.d** ou **hh:mm:ss**; valores validados dentro do vídeo.
- Novo PiP inicia após o término mais tardio dos PiPs anteriores; se o vídeo acaba, pergunta antes de permitir coincidência temporal.
- Aviso de conflito espacial-temporal: reorganizar, manter sobreposição com confirmação ou cancelar. Aprovação é persistida por PAR de PiPs, não aplicada indiscriminadamente.
- O tempo final do vídeo principal não é editado pelo campo de busca; esse campo move somente o cursor. Para aumentar a duração, edite/adicionar vídeos no projeto.

## Histórico v0.1.12: corte mais direto e controles compactos
- Miniaturas imediatamente abaixo do vídeo, **sem RangeSlider sobre as imagens**. Cada miniatura é tocável para saltar ao ponto aproximado; uma única barra separada permite ajuste fino.
- Removido o campo separado “Ir para”. O campo **Tempo** fica ao lado da reprodução e, a cada valor válido digitado com a máscara `mm:ss.d`, o cursor já salta, sem botão de aplicar.
- Entradas compactas de início/fim também aceitam digitação direta; somente o botão **Aplicar corte** modifica o projeto.
- **Marcar fim não é desativado silenciosamente:** quando o cursor não está após o início por pelo menos 250 ms, um aviso explica como resolver.
- Dividir, Desfazer, Anterior, Próximo e Excluir estão juntos em uma linha com ícones e rótulos pequenos. Dividir produz dois trechos na sequência; setas reordenam clipes quando há mais de um.
- Desfazer recupera a última alteração aplicada aos clipes nesta sessão: aplicar corte, dividir, mover ou excluir. Os arquivos originais permanecem intocados.
- Continua pendente validar compilação Android e testar reprodução, miniaturas e estados de entrada em hardware real.

## Histórico: 0.1.11: Cortar e unir e horários numéricos
- Tela Cortar e unir com prévia real do vídeo selecionado, reprodução/pausa, saltos de 1 s, cursor de reprodução independente, seis miniaturas carregadas sob demanda e linha do tempo única com seleção de início/fim por marcadores.
- Botões Marcar início e Marcar fim usando o ponto atual; editar os horários tocando no campo e digitando apenas números, com máscara automática `mm:ss.d` (exemplo: `00074` -> `00:07.4`). A máscara também vale para navegação da prévia e duração PiP.
- Aplicar corte grava a edição no projeto sem modificar o arquivo de mídia; Desfazer reverte a última aplicação de corte no clipe enquanto a tela permanecer aberta. Divisão, reordenação e exclusão preservadas.
- Miniaturas em baixa resolução e fora da thread principal para reduzir impacto de memória/fluidez. Prévia, edição temporal e exportação no aparelho ainda dependem de testes de CI e uso real.

## Novidades da 0.1.13: reprodução contínua e velocidade da prévia
- Em Cortar e unir, ExoPlayer repete o clipe automaticamente até o usuário pausar (REPEAT_MODE_ONE).
- Na tela principal de edição, a reprodução percorre todos os clipes e reinicia automaticamente (REPEAT_MODE_ALL).
- Botão compacto para velocidade de prévia: 0,25×, 0,5×, 0,75×, 1×, 1,25×, 1,5× e 2×. Não altera a velocidade nem a duração dos arquivos exportados.
- Áudio e vídeo PiP acompanham a velocidade da prévia principal no editor, mantendo ajuste separado do volume.
- Mini tutorial de 5 passos fica abaixo dos botões de edição de corte.
- Build e teste físico continuam pendentes; Media3 pode limitar decodificadores simultâneos no aparelho.

## Novidades acumuladas até 0.1.15
- Até **4 PiPs por projeto**, com vídeos ou imagens independentes, seleção de camada e opção de adicionar, substituir ou remover.
- Posição, escala 5–90%, duração, volume e silêncio independentes. Áudio desligado em um PiP não silencia os outros.
- Quatro botões quadrados e compactos Posição/Tamanho/Duração/Mídia **na mesma linha**. Cada botão abre um painel **ModalBottomSheet** deslizando da borda inferior, por cima dos controles e ações inferiores, sem alterar a altura ou a posição da prévia. Fechamento por gesto de arrastar, toque fora ou botão; altura máxima de 64% da tela, com conteúdo rolável.
- Mantidos os nove atalhos, escala 5–90% com entrada manual, início/fim e substituição de mídia. Os valores atualizam a prévia; o áudio individual continua fora da janela inferior.
- Prévia de até quatro PiPs ativos no instante do vídeo, edição por arrasto/pinça do selecionado, e faixas de tempo individuais no editor.
- Reposicionamento automático determinístico considerando retângulos, margens e **interseção temporal**. A camada editada recebe prioridade; as demais mudam de posição apenas se necessário. Se não houver espaço, bloqueia a mudança com aviso.
- Projeto JSON com `schemaVersion=4`, campo `overlays[]`, IDs persistentes e **migração de `pip` legado** sem descartar projetos antigos.
- `ExportEngine`: compõe as até quatro sequências visuais e trilhas de áudio independentes. **Compatibilidade real com 4 decodificadores, sincronismo e transparência depende de build e teste em aparelho**.
- Biblioteca de mídia MediaStore por tipo/pasta, alternativa SAF, e demais funcionalidades da 0.1.7 preservadas.

## Compilação e APK
1. Envie os arquivos da pasta extraída para a **raiz** do repositório GitHub.
2. Abra **Actions > Gerar APK Uai Cortes > Run workflow** ou envie alterações para `main`.
3. O workflow valida código, roda testes, Android Lint e `assembleRelease` com API 36.
4. Ao concluir, o APK estará em **Releases > v0.1.15 > Assets**, com o nome **`UaiCortes-v0.1.15.apk`** (arquivo direto). Também aparece em Actions Artifacts, onde o GitHub compacta automaticamente o download.

### Assinatura
Secrets exigidos para assinatura estável: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. Não inclua keystore, senhas ou arquivo TXT de Secrets no repositório. Sem Secrets, o script aceita assinatura debug provisória e pode não atualizar versões anteriores.

## Validações e limitações
- Validação estática executada. Testes Kotlin/JUnit, build Android e inspeção em aparelho ainda não foram executados neste ambiente.
- Testes unitários de posicionamento/intervalos adicionados ao projeto para rodar no CI; ainda sem compilação Android local.
- Ainda **não** foi realizado build Android Gradle nesta sessão nem teste em aparelho físico.
- A execução #3 (código 0.1.2) passou em Kotlin/assembleRelease, mas falhou no Lint; a 0.1.3 resolveu opt-in e a 0.1.4 adicionou upload obrigatório em Actions/Releases. O build 7 (0.1.6) falhou especificamente em `MediaBrowser.kt:165` por falta de opt-in Material 3; o build 15 (0.1.14) falhou em `MainActivity.kt` (OptIn Material 3 e escopo de maxWidth); a correção 0.1.15 aguarda nova compilação.
- A prévia de até quatro ExoPlayers de PiP pode apresentar limitação de decodificadores/defasagem em aparelhos reais. A exportação e transparência de imagens PNG também exigem testes.
- Dissolvência real ainda não é suportada. Fade preto/branco foram codificados para prévia/exportador e dependem de teste real. MP3/WAV não estão disponíveis na extração.
- Arquivos escolhidos no seletor do sistema usam URI persistente. Itens escolhidos pela biblioteca MediaStore dependem da permissão de leitura ativa e da existência do arquivo original; retirar a permissão pode tornar o PiP indisponível.

## Histórico
Ver `CHANGELOG.md`, `RELEASE_NOTES.md` e `docs/VALIDATION.md`.

## Novidades da v0.1.14
- Mesma origem horizontal para as faixas Vídeo, PiP, Áudio e Legenda, com cantos discretos em vez de cápsulas. Os clipes recebem menu inferior com Cortar, Duplicar, Mover, Excluir e volume individual, aplicado na prévia e exportação.
- Legendas manuais e importação SRT local. Ajuste de início/fim, texto, cor, tamanho e posição, renderização na prévia e sobreposição temporal no exportador.
- Transições por par de clipes: Corte direto e fade para preto/branco de 250 a 1000 ms. **Não existe dissolvência real** e os fades não reduzem a duração total.
- Extrair áudio de um clipe ou do projeto completo via Media3 (AAC dentro de MP4): salvar extensão M4A ou MP4 pelo seletor SAF e opcionalmente colocar na timeline.
- Áudio extra pode ser salvo em `audioTracks[]`; o campo antigo `music` continua legível. Novo JSON `schemaVersion=4`, preservando as listas de até quatro PiPs.
- **Limites:** não há codificador MP3 ou WAV. Algumas fontes sem faixa de áudio podem falhar na extração, com aviso. GPU, mixagem e combinações de PiPs/transições ainda precisam de build Gradle/CI e teste real.


## Correção da v0.1.15 · build 15

- Corrigido `@kotlin.OptIn(ExperimentalMaterial3Api::class)` nos composables `EditorScreen` e `PipScreen`, usados nos Bottom Sheets. O `@OptIn` AndroidX existente permanece somente para APIs Media3 `UnstableApi`.
- `TimelineRow` captura `maxWidth` em `BoxWithConstraints` antes do `Row` interno, evitando erro de receptor implícito na largura dos clipes.
- Atualizados Gradle, CI, validação de fonte, informações do aplicativo, notas de release e documentação para v0.1.15 / versionCode 16.
- **A validar:** executar Actions build 16 para confirmar compilação Kotlin Debug/Release, lint, APK e comportamentos em aparelho.
