## v0.1.58 (versionCode 59) · 30/10/2026
- **Correção do workflow 51:** adicionados em `MainActivity.kt` os imports `kotlinx.coroutines.launch` e `kotlinx.coroutines.CancellationException`, ausentes na importação de vídeos assíncrona da v0.1.57.
- **Compilação:** resolve as referências não encontradas que bloquearam `:app:compileDebugKotlin` e `:app:compileReleaseKotlin`. O erro de uso de `withContext` fora da coroutine era consequência da referência `launch` não resolvida.
- **Prevenção de regressão:** `tools/validate_source.py` verifica a presença única desses imports, `Dispatchers.IO`, `withContext` e a propagação de `CancellationException`.
- **Compatibilidade:** mantidos `schemaVersion=4`, bitrates/qualidade da v0.1.56, importação assíncrona e os controles atuais de projeto, sem mudar interfaces ou exigir novas permissões.
- **Validação:** inspeção dos logs do workflow 51, validação estrutural, sintaxe Kotlin e compilação isolada de API de corrotinas; Gradle/Android completo e testes em aparelho permanecem a confirmar.

## v0.1.57 (versionCode 58) · 30/10/2026
- **Correção do workflow 50:** teste `EditorWorkspaceStateTest.deletedSelectionsAreRevalidatedAgainstPersistedProject` corrigido: dois clipes possuem uma junção de índice **0**, não 1. Testes adicionais cobrem zero, um, dois e três clipes e revalidação após exclusão.
- **Transições robustas:** `TransitionBoundaryPolicy` centraliza o limite de junções; a prévia usa índice validado mesmo quando o projeto muda mantendo o ID. O editor atualiza a seleção na alteração do projeto, não apenas do identificador.
- **Importação responsiva:** metadados de vídeos importados são consultados em `Dispatchers.IO`; a interface mostra progresso indeterminado, impede duas importações concorrentes e descarta resultado atrasado se o projeto ativo mudou.
- **Mídia inválida:** novas importações usam duração verificada (0 quando indisponível), evitando criar clipes fictícios de 1 segundo. Fallback legado de outros fluxos permanece por compatibilidade.
- **Compatibilidade:** `schemaVersion=4` preservado, qualidade de exportação da 0.1.56 mantida, versionName/versionCode, documentação e workflow sincronizados.
- **Validação:** parser Kotlin, verificação estrutural e execução de testes JVM isolados; Gradle/Android completo e reprodução/exportação reais dependem de CI/aparelho.

## v0.1.56 (versionCode 57) · 30/10/2026
- Exportação de projetos: H.264 com bitrate solicitado de 8 Mb/s (720p) ou 18 Mb/s (1080p), AAC 192 kb/s e fallback do codec mantido.
- Qualidade padrão de novos usuários: 1080p; configurações existentes do usuário são preservadas.
- Animação de imagens: PNG temporário sem perdas (antes JPEG 94), mesmo bitrate de H.264, checagem estimada de espaço para MP4 temporário, cópia MediaStore e imagens preparadas.
- Interface de exportação informa bitrate solicitado e espaço estimado (não garantidos); registro técnico inclui bitrate real reportado pelo Media3.
- Resolução de origem não é recuperada por upscale. Pendente: compilação Android, teste no aparelho e comparação visual objetiva com MP4 da 0.1.55.

## v0.1.55 (versionCode 56) · 30/10/2026
- **Operações de projeto reutilizáveis:** `ProjectController` e `ProjectRepository` separam carregar, abrir por ID, salvar, criar/estender e excluir do código de interface. Atualização do estado acontece somente depois de `save/delete` no armazenamento; o backup e o JSON `schemaVersion=4` continuam em `ProjectStore`.
- **PiP desacoplado:** `OverlayOperations` concentra inserção sequencial, limite de quatro camadas, substituição por ID, cálculo de colisões, autorização explícita para sobrepor e remoção/seleção seguinte. A Activity mantém avisos, confirmação e chamadas de gravação.
- **Importações compartilhadas:** `MediaImportController` unifica consultas locais de metadados e pedidos de permissão SAF. `MediaImportRules` trata seleção única de imagens (máximo 80), clipes válidos e faixa de áudio dentro do projeto; entrada pela galeria e documentos utiliza o mesmo fluxo.
- **Testes e limites:** adicionados testes JUnit de projetos, PiP e importação, validação estrutural e compilação Kotlin/JVM isolada de regras. Não houve build Gradle/Android nem teste de exportação ou interface em aparelho.
- Documentação: `docs/ARQUITETURA_REFATORACAO_0.1.55.md` e `docs/VALIDATION_0.1.55.md`.

## v0.1.54 (versionCode 55) · 30/10/2026
- **Continuação da refatoração:** `EditorWorkspaceState` agora utiliza `rememberSaveable` e salva uma representação compacta (rota, seleções e posição), sem incluir JSON, vídeos ou fotos no `SavedStateRegistry`.
- **Projeto por ID:** o projeto ativo é localizado novamente no `ProjectStore` após recriação da Activity; IDs removidos e posições fora da duração são normalizados, sem alterar a estrutura `schemaVersion=4`.
- **Navegação reutilizável:** `EditorNavigation` concentra rotas, ação Voltar e tratamento de restauração sem projeto. O retorno entre ferramentas mantém as mesmas regras da versão anterior.
- **Fluxos de seleção:** origem/ação da galeria, destino de animação, URIs selecionadas de imagens (máximo 80) e vídeo para extração são salvos quando o Android fornece estado de instância.
- **Testes:** novos testes JUnit para snapshot, restauração sem projeto, seleções inválidas e navegação. Testes Gradle/Android e rotação física ainda pendentes; exportação e transcrição em andamento não são retomadas automaticamente.
- Documentação: `docs/ARQUITETURA_REFATORACAO_0.1.54.md` e `docs/VALIDATION_0.1.54.md`.

## v0.1.53 (versionCode 54) · 30/10/2026
- **Refatoração de base:** `MainActivity.kt` reduzida à inicialização/coordenação. Telas do editor, cortes, áudio, PiP, transições, legendas, exportação, marca d'água e timeline extraídas para arquivos Kotlin dedicados, sem mudar os fluxos visuais.
- **Estado compartilhado:** `EditorWorkspaceState` concentra navegação e seleções transitórias de clipes, PiPs, áudio, legendas e posição. Rascunhos e projetos continuam em seus armazenamentos próprios; não foi adicionada restauração automática de processo.
- **Exportação reutilizável:** `GalleryVideoSaver` publica MP4s de ambos os motores no MediaStore e só devolve a URI após concluir a cópia; elimina implementação duplicada e remove o item parcial ao falhar.
- **Segurança dos projetos:** `ProjectCollectionCodec` e `ProjectStore` preservam cópia anterior válida e bloqueiam `save/delete` se o cadastro e a cópia estiverem ilegíveis, evitando substituir dados por lista vazia. JSON de projetos continua `schemaVersion=4`.
- **Verificações:** validador estrutural atualizado e testes unitários acrescentados para codec/recuperação; sintaxe Kotlin inspecionada. Build Android/Gradle, testes unitários completos e verificação de MP4 em aparelho permanecem pendentes.
- Veja `docs/ARQUITETURA_REFATORACAO_0.1.53.md` e `docs/VALIDATION_0.1.53.md`.

## v0.1.52 (versionCode 53) · 30/10/2026
- Animar Imagens: ajustes unificados com chips compactos, mesma confirmação de salvamento MP4 usada no editor principal e atalho para continuar editando com áudio, narração, legendas, PiP e cortes.
- Novo **Salvar** no topo guarda a ordem das fotos e opções num rascunho local separado dos projetos de vídeo, retornando à Home ou ao editor de origem. Ao reabrir Animar Imagens, o rascunho é recuperado.
- Voltar com alterações não salvas oferece Salvar e voltar / Descartar / Continuar. O botão permanece disponível após exportações; etapa final de gravação bloqueia saída com explicação.
- Ao renderizar, o rascunho é gravado antes de iniciar o Media3. Arquivos de mídia originais não são copiados nem excluídos.
- Os recursos completos de áudio e timeline continuam no editor existente **após gerar o MP4**; o rascunho de fotos não é um MP4 nem um projeto de vídeo. Teste físico pendente.

## v0.1.51 (versionCode 52) · 30/10/2026
- Animar Imagens: corrigido o NPE do `LinearProgressIndicator` removendo `progress!!` e usando percentual imutável limitado a 0–100%, barra indeterminada quando ausente.
- Prévia com área compacta de 238 dp e proporção preservada, cartões menores, nomes curtos e controles **Ajustes/Animar** lado a lado.
- Alça de arrastar reposiciona a foto durante o gesto e permite rolagem nas bordas, mantendo botões subir/descer/remover e ordem da galeria.
- Identidade Uai Cortes no progresso, mantendo Media3 apenas no código e nos diagnósticos.
- Novo teste unitário do progresso; compilação Android, exportação MP4 e testes de gesto no aparelho ainda pendentes.
- **Animar Imagens offline** no menu inicial e nas ferramentas do editor: seleção múltipla na galeria, reordenação por arrasto e setas, movimentos de câmera automáticos, prévia e exportação MP4 H.264 com Media3.
- Ajustes opcionais de intensidade, duração (3/5/8s), formato (9:16/16:9/1:1), 720p/1080p e transições (corte/preto/branco/auto). Padrões: 5s, 9:16, 720p, 30 FPS.
- Salvamento no MediaStore, cancelamento, progresso quando disponível, posterior inclusão na timeline com áudio e efeitos; sem rede ou novo modelo.
- Segurança: cópias temporárias, fotos originais preservadas, sem alegação de animação corporal por IA ou paralaxe 2.5D. Veja `docs/ANIMAR_IMAGENS.md`.
- Compatibilidade: projetos antigos e schemaVersion 4 preservados. Compilação e testes de exportação no Android pendentes.

## v0.1.49 (versionCode 50) · 27/10/2026
- **Legendas automáticas offline (Vosk)**: nova tela acessível em Legendas → Auto; tutorial integrado, download opcional do modelo pequeno de português (~31 MB) no site oficial, decodificação de áudio dos vídeos, respeito a cortes e tempo de projeto, prévia/revisão individual antes de aplicar. Limite inicial de 10 minutos por operação; sem upload de áudio. Modelo extraído com proteção contra caminhos inválidos e limite de tamanho. A qualidade do reconhecimento depende da fala e do ruído; compilação e teste em Android ainda pendentes.
- **Transições de movimento**: Zoom cinematográfico, Recuo suave e Deslocamento lateral são efeitos de transformação por quadro Media3, acompanhados de prévia ilustrada. Não são crossfade nem dissolvência real entre clipes.
- **Projetos**: miniatura do primeiro quadro por projeto (com fallback) e confirmação explícita antes de excluir; vídeos originais não são apagados.
- Compatibilidade: schemaVersion 4 preservado; versionCode 50 e release dos cinco APKs pelo workflow. Não declarar APK/ASR testado até build real e teste em aparelho.

## v0.1.48 (versionCode 49) · 27/10/2026
- **Marca d'água em texto:** novo cartão no carrossel inferior do editor, com tela de edição própria, prévia do vídeo, texto até 80 caracteres, três cores, tamanho 12–36 e opacidade de 10% a 100%.
- Nove posições no quadro, usando coordenadas normalizadas do vídeo. **Aplicar** salva no projeto e volta ao editor; **Remover** apaga só a marca, sem tocar no texto das legendas ou nos arquivos originais.
- A marca ocupa a duração inteira do projeto, em camada separada das legendas SRT/animadas. A prévia do editor, a prévia de exportação e o Media3 Transformer receberam a mesma camada; projetos antigos sem `textWatermark` continuam válidos no schemaVersion 4.
- **Legendas automáticas:** estudadas as alternativas offline (whisper.cpp, Vosk) e online, mas transcrição automática ainda não foi implementada nesta versão. Exige decodificação de áudio, marcação temporal, escolha do modelo, revisão e controle de desempenho.
- Testes Kotlin isolados e validação estrutural incluídos. Compilação Android completa e conferência de MP4 em aparelho permanecem pendentes.

## v0.1.47 (versionCode 48) · 28/10/2026
- **Galeria interna:** corrigida a falha `The coroutine scope left the composition`. Corrotinas canceladas por troca de tela/permissão são propagadas e deixam de aparecer como erro de carregamento da mídia.
- Indicador de carregamento não é encerrado por consulta antiga cancelada; troca de permissões limpa o estado de erro. Em falhas reais, exibe **Tentar novamente** e **Procurar no Android**, sem mostrar caminhos de mídia ou detalhes internos.
- Falhas reais de consulta e permissão registradas no Diagnóstico (quando ativado), com mensagens genéricas na interface. O cabeçalho TXT passa a ler a versão real instalada, em vez de exibir sempre 0.1.41. Preservados filtros, miniaturas, seleção múltipla, SAF e projetos existentes.
- Diagnóstico TXT (cujo cabeçalho era fixo em 0.1.41, independentemente da versão instalada) também mostrou `SecurityException` no player do editor; esse incidente é distinto da falha de ciclo de vida da galeria. Pode exigir nova seleção da mídia e autorização no Android.
- Testes unitários de mensagens de erro, regressão estrutural de cancelamento, versionamento sequencial. Build Android e teste real de concessão/revogação de permissões permanecem pendentes.

## v0.1.46 (versionCode 47) · 27/10/2026
- Transições: nove opções, mantendo Corte direto, Fade preto e Fade branco e adicionando Flash branco, Flash preto, Fade azul, Fade dourado, Preto e branco e Sépia. Usam RgbMatrix do Media3 por quadro, sem sobrepor sequências de vídeo.
- Nova seleção de efeitos em faixa horizontal, mini prévia animada a partir de quadros dos dois clipes e duração ajustável para efeitos ativos.
- Escolhas são rascunhos até **Aplicar**. Botão inferior **Aplicar** persiste e retorna ao editor; **Aplicar a todas as junções** replica o efeito/duração nos pares adjacentes, sem alterar clipes ou mídia original.
- Sem migração do schemaVersion 4; testes de matrizes e aplicação em lote adicionados. Exportação e aparência física ainda exigem build CI e teste Android.

## v0.1.45 (versionCode 46) · 27/10/2026
- Exportação em tela cheia ao iniciar “Salvar vídeo”, com etapa Preparando/Processando/Salvando, nome do MP4, qualidade e cronômetro.
- Percentual vem de `Transformer.getProgress` somente quando disponível; indicador indeterminado nos demais estados. Sem ETA fictícia.
- Confirmação de cancelamento antes de descartar o MP4 parcial; o projeto e as mídias originais são preservados. Ao copiar para a galeria, o cancelamento é bloqueado para não interromper a gravação.
- Mantida a confirmação em tela cheia **Vídeo salvo!** somente após finalizar o MediaStore. Exportação não foi validada fisicamente neste ambiente.

# Uai Cortes 0.1.53

Editor offline Android nativo em Kotlin, Jetpack Compose e Media3.

## v0.1.44 (versionCode 45) · 27/10/2026
- Galeria interna MediaStore integrada às entradas de mídia visual: Novo projeto, Adicionar vídeo, PiP (adicionar/substituir) e vídeo externo para Extração de áudio.
- Filtros Recentes/Vídeos/Imagens conforme finalidade, busca por nome/pasta, pastas, miniaturas sob demanda, seleção múltipla ordenada para clipes e atualização da biblioteca.
- Permissões específicas por tipo, suporte ao acesso parcial do Android 14+, reavaliação ao retomar e seletor do Android preservado para fontes não indexadas. Sem cópia automática de arquivos grandes.
- Projetos continuam em schemaVersion 4; testes unitários de busca/filtros e validação das rotas de importação.

## v0.1.43 (versionCode 44) · 27/10/2026
- Corrigido erro de compilação do workflow **Gerar APK Uai Cortes 37**: `MainActivity.kt` referenciava `savedName` e `savedVideo` na confirmação de exportação sem declarar o estado no escopo de `ExportScreen`.
- Estados `remember(p.id)` adicionados antes do callback `onSuccess`, com `savedVideo` explicitamente tipado como `Uri?`; a confirmação em tela cheia, Abrir, Compartilhar, Voltar ao editor e Fechar permanecem.
- Validação estrutural ganhou regressão específica para essas declarações e sua ordem; sem mudanças nos projetos, legendas, motor Media3 ou formato do MP4. Compilação Android e teste físico continuam pendentes.

## v0.1.42 (versionCode 43) · 27/10/2026
- Exportação de legendas: `TimedSubtitleOverlay` respeita largura máxima de 90% do quadro, com quebras de linha automáticas e tamanho em proporção à largura real do vídeo, em vez de bitmap horizontal ilimitado. Preserve tamanho, posição, efeitos e tempos originais do projeto.
- `TextOverlay.getBitmap` substituído por StaticLayout com largura controlada e cache por quadro; legenda sem texto continua transparente, protegida contra imagem 0×0.
- Sucesso de exportação em tela cheia, somente após salvar no MediaStore, com Abrir, Compartilhar, Voltar ao editor e Fechar; falhas continuam detalhadas na tela de exportação.
- Não foi possível realizar exportação MP4 em aparelho neste ambiente; confirmar fidelidade da prévia e os efeitos no vídeo físico, vertical e horizontal.

## v0.1.41 (versionCode 42) · 21/10/2026
- Nova seção **Configurações > Diagnóstico**, desligada inicialmente e ativada manualmente, para capturar data e hora, tela ativa, toques aproximados (grade de 5%) e ações de navegação/salvamento.
- Captura falhas não tratadas, erros Media3 dos players, código/causa categorizada da exportação, extração de áudio e falhas ao salvar o relatório; logs internos limitados a 512 KiB com rotação.
- Visualização local, exportação TXT pelo seletor SAF, compartilhamento explícito pelo Android Sharesheet com FileProvider e exclusão com confirmação. Nenhum upload automático.
- Relatórios omitem textos digitados, legenda, nomes e URIs de mídia, IDs de projetos e conteúdo de arquivos; incluem modelo do aparelho/Android quando exportados. Modo não substitui o Logcat integral ou testes reais no dispositivo.
- Sem mudanças no schemaVersion 4, algoritmos de corte e codificação, projetos salvos e segredos. Build Android e testes físicos pendentes.

## v0.1.40 (versionCode 41) · 21/10/2026
- Corrigida provável causa do erro de exportação Media3 `ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED` com `width and height must be > 0`: `TimedSubtitleOverlay.getText()` retornava string vazia nos intervalos sem legenda e no primeiro frame de `Letra por letra`. O `TextOverlay` do Media3 tenta criar bitmap com largura zero nesse caso.
- A exportação usa caractere NBSP transparente de largura positiva quando nenhum texto deve aparecer, e define alpha 0 no overlay inativo; nenhum caractere adicional é gravado nos textos ou arquivos SRT.
- Acrescentados testes de regressão para legendas vazias, espaços/quebras de linha e texto visível. Mensagem de erro específica para falha de composição de quadro.
- Preservados corte, áudio, PiP, efeitos, projeto JSON schemaVersion 4 e o comportamento das telas. Compilação Android e exportação em aparelho ainda pendentes.

## v0.1.39 (versionCode 40) · 21/10/2026
- Corrigida configuração de duração de itens Media3 Transformer: não informar a duração *recortada* como se fosse a duração original da fonte em vídeos, PiPs e áudios. A biblioteca lê a duração intrínseca das mídias.
- Exportação: valida leitura das URIs locais e limites dos cortes antes do processamento, verificando espaço mínimo temporário; em caso de falha apresenta orientação mais específica, código Media3 e detalhe da causa quando disponível (também Logcat).
- Prévia Exportar: exibe legendas existentes com animações e estilo, camadas PiP e fades; anteriormente reproduzia somente o vídeo bruto, confundindo o usuário.
- A prévia ainda não garante fidelidade de áudio/mixagem; conferir o MP4 real depois do build e os códigos retornados no aparelho. "Asset loader error" sem código/causa não basta para diagnóstico definitivo.
- Mantidos projetos e esquema JSON 4, sem alterar arquivos originais. Compilação Gradle, exportação real no aparelho e conferência de legendas/PiPs pendentes.

## v0.1.38 (versionCode 39) · 21/10/2026
- Legendas: novo botão compacto **Salvar** na mesma faixa de **Nova** e **SRT**. Salva projeto editável e retorna à Home, não exporta MP4.
- Pop-up: efeitos de revelação lado a lado em carrossel circular horizontal (itens virtualizados), sem coluna longa; o efeito ativo mantém seleção.
- Áudio: seção **Mixagem** inicialmente recolhida para evitar controles de volume de todos os vídeos; cada faixa adicional exibe título, volume e edição em uma linha e abre o slider só quando solicitado.
- Exclusão de faixa de áudio agora pede confirmação e preserva o arquivo de origem; ao excluir uma faixa em edição o rascunho é fechado.
- Mantidos formato, volume e recorte reais já existentes; `schemaVersion=4` permanece. Validar UI, build Gradle, prévia e MP4 em aparelho.

## v0.1.37 (versionCode 38) · 21/10/2026
- Tela Legendas: editor em diálogo compacto com prévia ilustrativa de estilo, texto, início/fim lado a lado, botão Estilo, Animação, Excluir e Salvar.
- Rascunho local: alterações no pop-up apenas são persistidas ao tocar em Salvar. Cancelar descarta as mudanças do diálogo.
- Novas legendas manuais e de SRT usam tamanho inicial de 12 sp; projeto legado sem atributo continua sendo lido com 22 sp para preservar aparência histórica.
- Aplicar a todas permite escolher aparência, posição e animação de forma independente, com confirmação antes da gravação; não altera textos, horários nem IDs.
- Animações offline: letra por letra, frase por frase, destaque progressivo de palavras, pop/zoom, fade e deslizar, com tempo compartilhado entre prévia Compose e exportação Media3. A sincronização é temporal aproximada dentro de cada legenda, não acompanha a fala automaticamente.
- Preserva schemaVersion 4; as novas propriedades opcionais de animação usam defaults compatíveis. Build Android, testes em dispositivo e fidelidade exata prévia/MP4 ainda pendentes.


## Correção de build da versão 0.1.36
- O workflow 31 falhou ao compilar Debug e Release: a importação SRT acessava `searchQuery` antes de sua declaração em Compose. A variável foi movida para o escopo anterior ao callback, sem mudança funcional na busca.
- `tools/validate_source.py` agora valida a ordem da declaração e uso no importador SRT, além de verificar os demais recursos.
- Versão sincronizada em Gradle, workflow, empacotamento, tela Sobre, validação e notas de release. Compilação Android após a correção e teste no dispositivo estão pendentes.

## Novidades da versão 0.1.35
- **Prévia com proporção automática:** telas Legendas, Cortar e unir, PiP e Exportar ajustam o quadro ao vídeo vertical, horizontal ou quadrado, sem esticá-lo; altura limitada pela tela. Editor principal também muda a moldura quando a reprodução passa para outro clipe de proporção diferente.
- **Correção das legendas na prévia:** texto respeita a largura real do quadro, reduz a fonte apenas na visualização quando a palavra mais longa não cabe, e mantém a caixa visível. Evita que “Francisco” se parta entre linhas por causa da prévia estreita.
- **Edição preservada:** posição x/y, tamanho escolhido, SRT, tempos e `schemaVersion=4` não mudam. Essa correção não reprocessa o vídeo e não promete identidade de renderização com o MP4 até teste em aparelho.
- **Testes:** novos casos de geometria para retrato, paisagem, quadrado, metadados inválidos e passagem de clipes.
- **Pendente:** compilar no GitHub Actions e comparar prévia/exportação no aparelho.

## Novidades da versão 0.1.34
- **Busca em legendas:** encontre rapidamente textos entre dezenas ou centenas de entradas, sem exigir acentos ou maiúsculas. Mostra total filtrado, permite limpar e mantém a numeração original.
- **Anterior / Próxima:** navegue diretamente entre legendas cronológicas na prévia fixa. Desabilita setas nas extremidades e atualiza seleção e posição do vídeo.
- **Exclusão com confirmação:** exige confirmação antes de remover a legenda selecionada; texto e projeto permanecem intactos ao cancelar.
- **Segurança temporal e importação:** novas legendas não ultrapassam o vídeo; SRT acima de 250 mil caracteres é rejeitado com erro em vez de ser importado parcialmente.
- `schemaVersion=4` preservado. Sem modificação de fontes, vídeo, trilhas de áudio ou processo de exportação.
- **Validação Android:** build CI, comportamento real em aparelho e fidelidade do MP4 continuam pendentes.

## Novidades da versão 0.1.33
- **Legendas compactas:** uma única timeline horizontal com janelas de dez segundos, carregadas sob demanda, substitui as trilhas individuais empilhadas. As posições e durações exibidas usam horários absolutos, incluindo legendas sobrepostas.
- **Prévia fixa no topo:** vídeo Media3/ExoPlayer com reprodução do trecho selecionado e legenda arrastável; ao tocar na legenda, busca seu início sem alterar o MP4 original.
- **Lista enxuta e virtualizada:** número, texto abreviado, horário e ícone de edição. Tocar em uma linha expande somente a legenda ativa, incluindo texto, horários e uma pista de arrasto única para ajustar início, fim e deslocamento.
- **Avançado sob demanda:** fontes, estilos, cores, sombras, contornos, posição fina e copiar aparência para todas continuam disponíveis, mas inicialmente recolhidos.
- Importação SRT, criação e exclusão de legenda, defaults legados, persistência de propriedades e exportação foram preservados. Validação Android completa e teste visual em aparelho ainda pendentes.

## Histórico da versão 0.1.32
- Na tela Cortar e unir, a faixa **Trechos separados** apresenta miniatura, horários e destaque da parte ativa; os cartões rolam horizontalmente e a seleção rola automaticamente até o trecho escolhido.
- **Dividir** seleciona a parte à direita e recupera o cursor no ponto exato da divisão, sem importar novamente o vídeo. Ao reabrir o projeto, o último ponto de divisão permanece disponível.
- Excluir escolhe o trecho vizinho; Desfazer restaura a divisão e o marcador anterior. Transições já configuradas na fronteira de um clipe são preservadas.
- O arquivo de vídeo original não é alterado; salvar o projeto e exportar MP4 continuam separados.
- A lógica tem testes unitários; interface e Gradle Android aguardam validação em dispositivo/CI.

## Histórico da versão 0.1.31
- Editor: ação **Salvar** virou o primeiro cartão na barra inferior de ferramentas. Ao tocar, persiste o projeto local e volta diretamente à tela Home, sem exportar MP4.
- Os cartões da barra inferior rolam horizontalmente, retornando ao primeiro após o último, nas duas direções (lista virtual extensa, iniciada no centro para simular ciclo contínuo). Nenhuma ferramenta foi removida.
- Cortar e unir: sexto cartão **Salvar** após **Excluir** na faixa de ações com rolagem lateral. Salvar confirma também um corte ainda não aplicado, se início/fim forem válidos, e retorna diretamente à Home; um intervalo inválido exibe aviso e não sai da tela.
- A saída **Exportar** continua separada do salvamento do projeto editável. O arquivo original não é modificado.
- Testes estruturais e testes unitários da nova lógica foram adicionados. Compilação Android e verificação visual no aparelho ainda estão pendentes.

## Versões e ferramentas
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`.
- `versionName = 0.1.53`, `versionCode = 54`.
- Kotlin 2.1.21, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- Media3 1.10.1, ExoPlayer e Transformer para composição H.264/AAC.
- O editor não exige conta; somente o download opcional do modelo de legendas automáticas exige internet. A biblioteca PiP solicita acesso às fotos/vídeos do Android, integral ou parcial conforme consentimento; o seletor de documentos continua como alternativa.

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

## Novidades acumuladas até 0.1.30
- Até **4 PiPs por projeto**, com vídeos ou imagens independentes, seleção de camada e opção de adicionar, substituir ou remover.
- Posição, escala 5–90%, duração, volume e silêncio independentes. Áudio desligado em um PiP não silencia os outros.
- Quatro botões quadrados e compactos Posição/Tamanho/Duração/Mídia **na mesma linha**. Cada botão abre um painel **ModalBottomSheet** deslizando da borda inferior, por cima dos controles e ações inferiores, sem alterar a altura ou a posição da prévia. Fechamento por gesto de arrastar, toque fora ou botão; altura máxima de 64% da tela, com conteúdo rolável.
- Mantidos os nove atalhos, escala 5–90% com entrada manual, início/fim e substituição de mídia. Os valores atualizam a prévia; o áudio individual continua fora da janela inferior.
- Prévia de até quatro PiPs ativos no instante do vídeo, edição por arrasto/pinça do selecionado, e faixas de tempo individuais no editor.
- Reposicionamento automático determinístico considerando retângulos, margens e **interseção temporal**. A camada editada recebe prioridade; as demais mudam de posição apenas se necessário. Se não houver espaço, bloqueia a mudança com aviso.
- Projeto JSON com `schemaVersion=4`, campo `overlays[]`, IDs persistentes e **migração de `pip` legado** sem descartar projetos antigos.
- `ExportEngine`: compõe as até quatro sequências visuais e trilhas de áudio independentes. **Compatibilidade real com 4 decodificadores, sincronismo e transparência depende de build e teste em aparelho**.
- Biblioteca de mídia MediaStore por tipo/pasta, alternativa SAF, e demais funcionalidades da 0.1.7 preservadas.

## v0.1.30 · Correção de compilação do contorno das legendas
- Corrigido `MainActivity.kt` para utilizar `StrokeJoin.Round`, enum disponível no Jetpack Compose, no lugar de `PathJoin.Round`, que causou `Unresolved reference` nas compilações Debug e Release do workflow 26.
- A geometria e a espessura do contorno continuam as mesmas. A mudança não altera JSON, presets, áudio, timeline, compatibilidade ou efeitos já configurados.
- Compilação da v0.1.30, lint e publicação dos cinco APKs devem ser confirmados pelo GitHub Actions.

## v0.1.29 · Sombras, entrelinhas e alinhamento das legendas
- Controle individual de sombra diagonal sem desfoque, com deslocamento de 0–6 dp (0 desativa) e cor preta ou branca.
- Espaçamento adicional entre linhas em 100–160% e alinhamento de linhas à esquerda, ao centro ou à direita para legendas com múltiplas linhas.
- A prévia Compose e a composição Media3 receberam o novo estilo, preservando o posicionamento normalizado X/Y. O contorno acompanha o alinhamento escolhido.
- Leitura de projetos antigos usa sombra desligada, espaçamento normal e centro. A ação de copiar aparência transfere também os três novos ajustes, sem alterar IDs, texto, horários ou posição.
- Incluídos `SubtitlePresentationTest` e validações de compatibilidade. Conferir a fidelidade visual no Android e no MP4 exportado antes de considerá-la comprovada.

## v0.1.28 · Fontes e contorno de legendas
- Três fontes locais do Android: Moderna (sans-serif), Serifada e Monoespaçada, sem download de arquivos ou permissão adicional.
- Contorno com intensidade de 0–4 dp e cor preta/branca, aplicável individualmente; prévia Compose com traço sobreposto e exportação Media3 por desenho de borda por linha.
- Os estilos podem ser copiados para todas as legendas sem alterar IDs, horários, conteúdo nem posição no vídeo.
- `SubtitleCue` persiste `fontFamily`, `outlineWidth` e `outlineColor` com padrões sem contorno e fonte Moderna para projetos antigos. Schema v4 preservado.
- A renderização exata, especialmente quebra de linha/alinhamento em fontes diferentes, precisa de teste comparativo em APK e MP4 reais.

## v0.1.27 · Estilos e aparência das legendas
- Cinco presets rápidos: Clássico, Destaque, Ciano, Limpo e Etiqueta, com visualização imediata na prévia e na imagem do vídeo.
- Controle individual de cor, tamanho, fundo (sem fundo/suave/escuro/claro) e negrito. Fundo transparente não cria tarja no MP4.
- A ação **Copiar aparência para todas** pede confirmação e muda somente cor/tamanho/fundo/negrito, sem afetar tempos, IDs, texto ou posição.
- `SubtitleCue` grava `backgroundColor`/`bold` no projeto e adota o aspecto clássico para legendas legadas sem esses campos; `schemaVersion=4` inalterado.
- Renderização real do MP4, fontes e contraste em aparelhos ainda requer testes no CI e no Android.

## v0.1.26 · Legendas editáveis na timeline
- Cada bloco da pista Legenda, na timeline principal, abre exatamente a legenda selecionada, sem depender da ordem de importação SRT.
- Na tela Legendas, cada texto tem sua pista individual: alça esquerda altera início, centro move mantendo duração e alça direita altera fim.
- Ajustes temporais aparecem durante o gesto e são gravados no projeto ao soltar; entradas numéricas de início/fim continuam disponíveis.
- Limites impedem ultrapassar início/fim do projeto e respeitam trechos curtos já importados. Os arquivos originais e a estrutura JSON permanecem intactos.
- O funcionamento visual e a exportação ainda precisam ser validados no Android.

## Compilação e APK
1. Envie os arquivos da pasta extraída para a **raiz** do repositório GitHub.
2. Abra **Actions > Gerar APK Uai Cortes > Run workflow** ou envie alterações para `main`.
3. O workflow valida código, roda testes, Android Lint e `assembleRelease` com API 36.
4. Ao concluir, o **GitHub Releases > v0.1.30 > Assets** oferece diretamente cinco arquivos `.apk` individuais, sem um Artifact adicional e sem ZIP de distribuição:
   - `UaiCortes-v0.1.30-arm32.apk` (`armeabi-v7a`)
   - `UaiCortes-v0.1.30-arm64.apk` (`arm64-v8a`)
   - `UaiCortes-v0.1.30-x86.apk` (`x86`)
   - `UaiCortes-v0.1.30-x86_64.apk` (`x86_64`)
   - `UaiCortes-v0.1.30-universal.apk` (qualquer arquitetura suportada)
5. Somente o APK **Release** é publicado. O gatilho por tags `v*` foi removido para impedir segundo build disparado pela tag criada no Release. O GitHub Actions mostra links diretos no resumo.
6. O mesmo `versionCode=31` é usado em todas as variantes; o APK universal não exige conhecer a arquitetura. Apenas um deve ser instalado por vez. O app atual usa majoritariamente código gerenciado e Media3, portanto tamanhos de APK por ABI podem ser muito próximos. **Não presumir que uma variante é menor antes de medir**.
7. O script `tools/package_apks.py` valida os cinco resultados do Gradle por `output-metadata.json`, assinatura, pacote, `versionCode` e SDK; falha se faltar alguma ABI ou houver duplicata. Não escolhe o primeiro APK encontrado.

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

## Atualização 0.1.22: build 16 e download por arquitetura
- Logs do build 16: o Kotlin compilou, mas `:app:lintDebug` falhou com `UnsafeOptInUsageError` em chamadas `AspectRatioFrameLayout.RESIZE_MODE_FIT` dentro de lambdas `AndroidView` em `MainActivity.kt` (lint encontrou 3 erros e 39 avisos).
- Chamadas instáveis de Media3 (`resizeMode`, `setShowBuffering`, shutter) centralizadas no helper `previewPlayer`, anotado com `androidx.annotation.OptIn` para deixar explícita a adesão a `UnstableApi`.
- `splits.abi` configurado para quatro ABIs e APK universal. Publicação unificada em Releases, sem Actions Artifacts duplicado ou gatilho por nova tag.
- **Pendente:** compilar e testar 0.1.22 no Actions, confirmar que lint passou e que os cinco APKs instalaram/funcionam nas arquiteturas compatíveis.

## v0.1.22
A tela Transições agora anima uma prévia baseada em quadros reais da junção. Não é reprodução contínua do vídeo nem crossfade.

## v0.1.24 · Recorte e posicionamento das faixas de áudio

- Cada faixa importada/extraída tem ação **Editar corte** na tela Áudio. A entrada e saída do arquivo e a posição inicial na timeline são editadas por controle deslizante e horário numérico.
- Pré-escuta independente do trecho selecionado; **Salvar corte** aplica alterações ou **Cancelar** mantém a faixa anterior. Exige pelo menos 250 ms e impede passar dos limites da fonte/projeto.
- `AudioTrack.sourceStartMs` persiste a posição de entrada no arquivo, por padrão zero em projetos antigos (sem migração destrutiva). Prévia do editor e exportação agora começam na mesma posição da fonte.
- Não inclui waveform, crossfade ou codificação MP3/WAV. Validar compilação e sincronismo de reprodução/exportação no Android.


## v0.1.24 · Timeline de áudio com gestos
- **Editor principal:** faixas extras de áudio são blocos arrastáveis horizontalmente para mover o instante em que começam. A posição é salva no projeto **ao soltar**. Um toque curto abre diretamente a faixa selecionada na tela Áudio.
- **Tela Áudio:** barra visual com **alça esquerda** para adiantar/retroceder o começo do trecho mantendo o término fixo no projeto, **centro** para mover o trecho completo e **alça direita** para ajustar o término da fonte.
- Ajustes gestuais são uma prévia local até o usuário tocar **Salvar corte** na tela Áudio; **Cancelar** abandona essas mudanças. Campos numéricos continuam disponíveis para ajustes finos e trechos muito curtos.
- Os gestos usam os limites da mídia de origem e do projeto (mínimo de 250 ms); não alteram os arquivos de mídia. Prévia e exportação continuam usando `AudioTrack.sourceStartMs`, `durationMs` e `startMs`.
- **Pendente:** execução GitHub Actions (`testDebugUnitTest`, `lintDebug`, `assembleRelease`) e teste físico de gestos, reprodução e MP4.


## v0.1.25 · Posição proporcional das legendas e ajuste fino do cursor
- A prévia do editor agora posiciona legendas proporcionalmente às dimensões **reais do quadro**, usando âncora inferior central como a exportação Media3. O antigo deslocamento fixo em dp foi eliminado.
- Tela Legendas: miniatura do quadro no início da legenda com texto arrastável. A posição é salva **ao soltar**; sliders e setas de 1% completam o ajuste. A miniatura pode estar indisponível se a mídia não for legível.
- Editor principal: passos de ±0,1 segundo e ±1 segundo na timeline para acertar o cursor em períodos curtos; tempos extremos são limitados ao projeto.
- Lógica pura `TimelinePrecision` e `SubtitlePlacement`, com testes de limites e coordenadas. Arquivo original, JSON/schema e comportamento da exportação permanecem intactos.
- Pendente: compilação Android, teste de arrasto em diferentes proporções de vídeo e comparação visual com MP4 real.
