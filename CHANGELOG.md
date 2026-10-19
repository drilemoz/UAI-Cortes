## v0.1.25 (versionCode 26) · 19/10/2026
- Melhorada a precisão de navegação no editor com passos de 100 ms e 1 s, limitados ao intervalo do projeto.
- Legendas posicionadas proporcionalmente ao tamanho do vídeo, sem deslocamentos fixos em dp. Prévia usa a âncora inferior central do exportador.
- Nova miniatura da legenda com arrasto e setas para ajustes de posição de 1%; aplica a posição ao concluir o gesto e mantém sliders existentes.
- Seleção de quadro do clipe correspondente ao tempo da legenda, sem alterar mídia original nem a estrutura JSON.
- Incluídos testes `TimelinePrecisionTest`; compilação Android e fidelidade da exportação ainda pendentes.

## v0.1.24 (versionCode 25) · 19/10/2026
- Timeline principal de áudio agora permite arrastar horizontalmente as faixas extras para alterar o início no projeto, persistindo só ao concluir o gesto.
- Toque em uma faixa seleciona e abre o editor de áudio diretamente nela.
- Editor de áudio com alças de recorte visual: esquerda (ajusta origem e tempo de entrada), centro (move), direita (ajusta fim do arquivo). Prévia dos valores até Salvar corte, preservando Cancelar.
- Lógica `AudioTimelineEdit` com testes Kotlin de limites do arquivo/projeto, duração mínima e conversão pixels/tempo.
- Compatibilidade JSON preservada; áudio legado e exportação não foram modificados nesta versão.
- Verificações locais não substituem Gradle/Android e testes em aparelho.

## v0.1.23 (versionCode 24) · 19/10/2026

- Faixas adicionais com entrada e saída no áudio de origem, posição na timeline, controles numéricos/deslizantes e pré-escuta separada.
- Exportação Media3 e prévia do editor respeitam `sourceStartMs`; projetos antigos permanecem compatíveis com origem em 0 ms.
- Limites de corte conservadores (mínimo de 250 ms) e testes de lógica/serialização adicionados.
- ZIP/validação estrutural: a compilação Android e teste físico desta versão continuam pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Pré-escuta de áudio para clipes individuais e vídeos externos, com reprodução, pausa, reinício e limite de recorte.
- Reprodução temporária isolada da timeline e liberação do player ao sair da tela.
- Pré-escuta de projeto inteiro ainda não implementada. Build Android e testes em aparelho pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Prévia animada de transições usando quadros reais extraídos dos clipes adjacentes, com Fade preto, Fade branco e Corte direto.
- Extração de quadros em IO, sem decodificação contínua ou áudio. A prévia não substitui reprodução/renderização integral.
- Mantidas as cinco variantes de APK e a exportação Media3 existente.
- Build Android e testes em aparelho pendentes.

Correção v0.1.22: o workflow 19 compilou Kotlin Debug/Release, executou testes e lint com sucesso, mas falhou no empacotamento porque tools/package_apks.py ainda esperava 0.1.17/versionCode 17. Constantes sincronizadas para 0.1.22/versionCode 23. Compilação e publicação da nova versão pendentes.

## 0.1.22 (versionCode 23) — 19/10/2026
- Corrigida compilação Kotlin do build 18: chamada de `TimelineRow` para a faixa PiP vazia agora passa explicitamente `onClip`.
- Nenhuma alteração na lógica de exportação ou transições.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

## 0.1.18 (versionCode 19) — 19/10/2026
- Extração de vídeo externo via SAF e recorte por início/fim.
- Posição X/Y de legendas persistida no projeto e aplicada à exportação.
- Implementação parcial: prévia de transições, indicadores na timeline e menu de ações ainda pendentes.

## 0.1.16 (versionCode 17) — 19/10/2026
- Correção do Lint do build 16: configuração instável do `PlayerView` isolada em função com opt-in AndroidX explícito; removidas chamadas Media3 instáveis de lambdas Compose.
- Configuração de build ABI splits `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` e universal, sem criar variantes Debug para distribuição.
- Correção de distribuição duplicada: o workflow não escuta mais tags criadas na publicação nem publica o mesmo APK como Actions Artifact. Uma Release com cinco APKs nomeados.
- Validação automatizada de cinco saídas Release, metadados de ABI, assinatura, pacote, targetSdk, versionName e versionCode. Atualizados docs e tela Sobre.
- APK/Android Lint ainda dependem de execução no GitHub Actions; testes físicos por arquitetura pendentes.

## 0.1.15 (versionCode 16) — 19/10/2026

- Corrigido build 15: `ModalBottomSheet` e `rememberModalBottomSheetState` exigem `@kotlin.OptIn(ExperimentalMaterial3Api::class)` nos componentes da edição principal e PiP.
- Captura explícita de `maxWidth` no `BoxWithConstraints` de `TimelineRow` para evitar erro Kotlin causado pelo `Row` aninhado.
- Metadados, CI e documentação sincronizados. Nenhuma funcionalidade de edição removida.
- Compilação Android completa permanece pendente.

## 0.1.14 (versionCode 15) — 19/10/2026
- Alinhamento unificado das faixas da timeline (rótulo de 60 dp, margem de 12 dp); blocos retangulares.
- Toque em clipe abre Bottom Sheet compacto: cortar, duplicar, mover, excluir com confirmação e volume individual; prévia passa a seguir volume individual.
- Legendas manuais e importação SRT: campo de texto, horários, cor, tamanho e posição; faixa própria e efeito temporal no exportador.
- Transições independentes por par adjacente: corte direto, fade preto, fade branco (0,25 a 1 s); sem falsa dissolvência ou alteração de duração.
- Áudio: faixas independentes, ajuste de volume e remoção, prévia; extração de áudio do projeto ou clipe em M4A ou MP4 contendo AAC, salvando no SAF, opção salvar e acrescentar à timeline.
- Projeto JSON schemaVersion 4 com migração de campos anteriores e testes de SRT, duração e fades.
- Validações locais de sintaxe e cálculos; build Android, exportação múltipla, mixagem, efeitos GPU e aparelho ainda pendentes.

## 0.1.13 (versionCode 14) — 18/10/2026
- Reprodução em loop automático até pausar: REPEAT_MODE_ONE no clipe de Cortar e unir e REPEAT_MODE_ALL na playlist do editor principal.
- Seletor compacto de velocidade em ambas as telas (0,25× a 2×), alterando somente a prévia; vídeo exportado preserva velocidade original.
- PiPs na prévia do editor recebem a mesma velocidade do vídeo principal, para limitar defasagem durante reprodução acelerada ou lenta.
- Mini tutorial de cinco orientações abaixo dos controles Cortar e unir, incluindo o uso das ferramentas e a diferença entre prévia e exportação.
- Testes novos do catálogo de velocidades, documentação e workflow atualizados. Build Android e teste físico pendentes.

## 0.1.12 (versionCode 13) — 18/10/2026
- Cortar e unir simplificado: miniaturas clicáveis diretamente abaixo da prévia, sem marcador duplo sobre as imagens; único controle fino de cursor separado.
- Campo Tempo move o cursor ao digitar números válidos, com a máscara `mm:ss.d` preservada; sem campo “Ir para”/botões adicionais.
- Início/fim editáveis diretamente, botões de marcação sempre clicáveis com explicação quando o fim não pode ser definido (mínimo 250 ms).
- Dividir, Desfazer, mover antes/depois e Excluir agrupados na mesma linha compacta; desfazer de uma operação também cobre divisão, reordenação e exclusão.
- Mantidos quatro PiPs e edição sem modificar os arquivos originais. Pendente build Gradle e teste físico.

## 0.1.11 (versionCode 12) — 18/10/2026
- Editor Cortar e unir com preview ExoPlayer do clipe, controles de reprodução/seek de 1 segundo e miniaturas amostradas em IO.
- Linha do tempo compacta com marcadores RangeSlider para início/fim e cursor independente; ações Marcar início/Marcar fim; edição em rascunho com Aplicar corte, Desfazer última aplicação, Dividir, Reordenar e Excluir.
- Entrada de horários numérica autoformatada `mm:ss.d` com controle de limite, compartilhada pela busca na prévia e editor PiP.
- Mantidos projeto não destrutivo, múltiplos PiPs e compatibilidade com projetos anteriores. Workflow e documentação sincronizados. Build Android/teste em aparelho pendentes.

## 0.1.10 (versionCode 11) — 18/10/2026
- Bottom Sheet sem alça superior, mantendo botão Fechar; menos margens e espaços na tela PiP e no painel inferior.
- Horários tocáveis: campo digitável de busca na prévia e editor principal; início/fim do PiP com valor digitável, validação e barra preservada.
- Novo PiP começa após o último término existente, sem usar o cursor do vídeo; quando o último já alcançou o fim do projeto, exige confirmação para adicionar em período coincidente.
- Conflitos ESPAÇO+TEMPO têm aviso e opções Reposicionar automaticamente, Manter sobreposição ou Cancelar. Permissões de sobreposição ficam registradas por par de IDs no JSON (schema 3), sem liberar conflitos com futuros PiPs.
- Mantidos quatro PiPs, controles separados de áudio e exportação Media3. Compilação Android e testes no aparelho continuam pendentes.

## 0.1.10 (versionCode 11) — 18/10/2026
- Reorganizadas as ferramentas Posição, Tamanho, Duração e Mídia em **uma única linha** com quatro botões quadrados compactos e ícones.
- Os ajustes agora abrem em **ModalBottomSheet** na borda inferior; aparecem sobre os controles, sem empurrar o vídeo. Altura limitada, rolagem interna, tema do app e fechamento por gesto, toque fora ou botão.
- Mantidos os controles completos (9 posições, tamanho 5–90%/manual, duração e mídia), seleção de até quatro PiPs, áudio independente, anticolisão e compatibilidade de projetos.
- Sincronizados Gradle, tela Sobre, workflow de APK, README, AGENTS, release e validações. Compilação Android e testes físicos pendentes.

## 0.1.8 (versionCode 9) — 18/10/2026
- PiPs múltiplos (até 4) em um projeto, com seleção e edição independentes.
- Interface quadrada expansível na própria tela (Posição/Tamanho/Duração/Mídia); removidos os pop-ups de configuração.
- Motor de colisões espacial e temporal com prioridade para o PiP editado e aviso quando não houver espaço livre.
- Linha do tempo dividida em uma faixa por PiP, posição de início e duração corretas.
- Prévia e composição Media3 das até quatro fontes visuais; áudio independente para cada vídeo PiP.
- Modelo JSON `overlays` e `schemaVersion=2`; leitura automática do campo `pip` dos projetos antigos.
- Documentação/workflow atualizados para 0.1.8 e versionCode 9; APK ainda depende de compilação CI.

## 0.1.7 (versionCode 8) — 18/10/2026
- Corrigida compilação Debug/Release relatada no workflow 7: `PrimaryTabRow` do Material 3 requer `@OptIn(ExperimentalMaterial3Api::class)` na `DeviceMediaBrowser`.
- Preservadas biblioteca de mídia PiP, controles de posição/tamanho/duração e áudio individual do PiP.
- Versionamento e publicação APK sincronizados (Gradle, tela Sobre, workflow, README, release, validador).
- Ainda requer validação de build Android completo no GitHub Actions e testes físicos.

## 0.1.6 (versionCode 7) — 18/10/2026
- Menu compacto para PiP: janelas de Posição (9 atalhos), Tamanho (5% a 90%, slider e entrada manual) e Duração (início/fim e faixa).
- Nova biblioteca local de mídia: abas Imagens/Vídeos, filtros por pasta, miniaturas em grade com espaço reduzido, ícone e tempo dos vídeos. Usa MediaStore, permissões Android e alternativa pelo seletor de arquivos.
- Áudio de vídeo PiP em cartão próprio: silenciar/ativar sem perder volume escolhido; o estado é persistido e respeitado na prévia e na exportação.
- Ajustes de geometria para 5%, testes para entrada manual e serialização do estado mudo, documentação e workflow sequenciais.
- Compilação Android e verificação em aparelho pendentes.

## 0.1.5 (versionCode 6) — 18/10/2026
- PiP com visualização do vídeo principal, posição rápida 3×3, exibição proporcional e tempos de início/fim.
- Corrigida a posição central e limites da área de arrasto, guias centrais e prévia reproduzível.
- Adicionados testes Kotlin da geometria e da janela temporal.
- Workflow e documentação atualizados para UaiCortes-v0.1.5.apk.

# Histórico de versões

## 0.1.3 (versionCode 4)
- Opt-in pontual do Media3 UnstableApi no indicador de buffering da prévia, corrigindo falha Lint #3; sem ocultar erros do Lint.

## 0.1.2 (versionCode 3)
- Correção dos imports Media3 no motor de exportação e validador estático.

## 0.1.1 (versionCode 2)
- Atualização do setup Android para remover pacote SDK legado.

## 0.1.0 (versionCode 1)
- Base nativa do Uai Cortes e telas iniciais do editor.
