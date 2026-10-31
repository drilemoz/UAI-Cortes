## v0.1.70 (versionCode 71) · 31/10/2026

- **Causa comprovada pelo diagnóstico, vídeo e ZIP real:** `vosk-model-small-pt-0.3.zip` contém `final.mdl`, `mfcc.conf` e grafos na raiz (layout Vosk legado). O instalador 0.1.69 exigia exclusivamente `am/final.mdl` + `conf/model.conf`, rejeitando o arquivo oficial em `extractAndInstall`.
- **Correção:** `VoskModelLayout` identifica modelos legados e modernos, exigindo modelo acústico, configuração MFCC e grafo não vazios. Os caminhos nativos são preservados, sem alterar estrutura original nem criar configuração artificial. Mesma validação para download interno, importação SAF e detecção de modelo instalado.
- **Interface:** três botões de instalação em uma única linha (Baixar, Navegador, Importar ZIP), com ícones e rótulos compactos. Cancelamento e progresso continuam separados.
- **Testes:** três novos casos de layout; inspeção do arquivo ZIP real enviado; validador estrutural atualizado. Sem build Android completo/teste do Vosk nativo em aparelho nesta entrega.
- **Compatibilidade:** `schemaVersion=4`, dados de projetos, PiP, exportação e reconhecimento offline mantidos.

## v0.1.69 (versionCode 70) · 31/10/2026

- **Vosk:** alternativa ao download interno: abrir o endereço oficial no navegador e importar o ZIP com seletor Android (SAF), sem extração manual.
- **Instalação:** regras de tamanho, verificação de arquivos essenciais, descompactação com bloqueio de caminhos inseguros, limpeza de arquivos temporários e exclusão mútua entre instalações.
- **Progresso:** bytes reais recebidos e percentual quando o servidor informa tamanho; indicador indeterminado quando não informa.
- **Erros:** diferenciar timeout, falha de DNS, HTTP inválido e ZIP incorreto, sem alegar que a falha original no aparelho foi reproduzida.
- **Preservação:** transcrição offline, `schemaVersion=4`, projetos, PiP e qualidade de exportação inalterados.
- **Validação:** parser Kotlin, regras isoladas de progresso e estrutura de projeto; download real, build Android e uso com Vosk dependem de testes no dispositivo/CI.

## v0.1.68 (versionCode 69) · 31/10/2026
- **Novidades na primeira abertura:** uma tela visual apresenta as atualizações e aparece automaticamente somente se houver mudanças ainda não vistas. A confirmação é armazenada localmente por assinatura do conteúdo das notas, não apenas por número de versão.
- **Informações sincronizadas:** `AppReleaseNotes` é a fonte única usada por `Sobre` e `Novidades`, incluindo as atualizações recentes. Removido o texto antigo da tela Sobre e corrigida a exibição do `versionCode`.
- **Acesso manual:** Configurações > Novidades e Sobre > Ver novidades permitem consultar a lista sem forçar nova abertura automática. Ao tocar em Continuar ou Voltar, a leitura é registrada.
- **Regressões:** testes verificam identidade das listas, reapresentação apenas após novidades reais, restauração da rota e retorno correto. `schemaVersion=4`, vídeos, projetos, exportação, PiP e Vosk preservados.
- **Pendente:** build Gradle/Android, teste no aparelho, rotação e conferência visual da tela nos temas claro e AMOLED.

## v0.1.67 (versionCode 68) · 31/10/2026

- Tela inicial: lixeira direta por projeto e ação de excluir todos com confirmação da quantidade completa; exclusão atômica e sem apagar vídeos originais/MP4s exportados.
- Criação de projeto pede nome antes de selecionar os vídeos; o nome persistido é usado como sugestão do MP4 (sanitização de nome inválido) e continua editável em Exportar.
- Galeria: a busca grande virou uma lupa opcional no cabeçalho; aba única Vídeos/Imagens não aparece quando o filtro já fixa esse tipo; filtros de pasta e SAF continuam acessíveis.
- Legendas automáticas: configuração/instalação Vosk transferida para Configurações > Reconhecimento de voz, com progresso numérico percentual, etapas de verificação/extração, cancelamento e mensagens de erro. Link oficial preservado, teste de download físico pendente.
- PiP: seletor de aplicação temporal aos clipes reais do projeto, projeto inteiro ou cursor ao final; vídeos PiP não são estendidos além da duração original sem suporte de loop. Dois campos compactos e uma única barra de intervalo substituem sliders duplicados.
- Testes e validador ampliados. `schemaVersion=4` preservado. Compilação Android e validação em dispositivo pendentes.

## v0.1.66 (versionCode 67) · 30/10/2026

- **Rascunhos seguros do Animar Imagens:** codec separado valida URIs e opções e distingue primeira execução, rascunho íntegro, backup recuperável e cadastro danificado.
- **Backup consistente:** antes de salvar, a última versão JSON válida permanece como backup; se ambas as cópias estiverem danificadas, a gravação é bloqueada até o usuário confirmar um reinício explícito. Não apaga imagens originais.
- **Permissões e fotos indisponíveis:** verificação de leitura local em `Dispatchers.IO` antes da exportação; referências sem acesso permanecem no rascunho, são comunicadas na tela e só são removidas por ação explícita.
- **Testes de regressão:** novos casos para formato anterior, JSON inválido, ordem, preservação do backup e acesso a fotos. Acessos persistentes do SAF continuam solicitados pelo seletor Android.
- `schemaVersion=4` preservado, sem mudanças no Media3, bitrate ou no catálogo de projetos. Build Android completo, testes JUnit Gradle e testes reais de permissão no aparelho ainda pendentes.

## v0.1.65 (versionCode 66) · 30/10/2026

- **Persistência e recuperação de projetos:** o cadastro de projetos agora distingue uma instalação nova de uma chave principal ausente/corrompida. A cópia de segurança é examinada antes de assumir que não há projetos.
- **Escrita protegida:** o backup recebe somente o último JSON previamente validado; IDs em branco ou duplicados não podem ser persistidos. Dados ilegíveis continuam bloqueando gravações destrutivas.
- **Aviso de armazenamento:** o aplicativo informa quando recuperou o backup ou quando ambas as cópias estão ilegíveis. Não apresenta um erro de leitura silenciosamente como se os projetos tivessem sido excluídos.
- **Falhas de salvamento:** a interface informa o erro e não executa automaticamente o comando de voltar para a tela inicial quando a gravação falha; a exclusão também informa erro sem esconder a falha do usuário.
- `schemaVersion=4` preservado. Nenhuma migração de projetos nem alteração nos motores de exportação. Compilação Android completa e testes físicos permanecem pendentes.

## v0.1.64 (versionCode 65) · 30/10/2026

**Editor, reprodução e timeline.** Avançar/voltar 5 segundos agora considera a duração total do projeto, inclusive cruzando clipes. A barra de progresso acompanha o dedo durante a busca e não deve sofrer correções do player enquanto está sendo arrastada. A posição salva é retomada ao preparar a prévia e a velocidade de visualização passa a ser gerenciada pelo ViewModel. Os recursos existentes e o formato dos projetos foram preservados. Compilação Android completa e testes em aparelho continuam pendentes.

## v0.1.63 (versionCode 64) · 30/10/2026
- **Animar Imagens modularizado:** `ImageAnimationTaskViewModel` assume o motor Media3, progresso, cancelamento e confirmação do MP4; a interface só apresenta estados e opções.
- **Reaproveitamento:** mesmo `ExportTaskCoordinator` do exportador principal com tickets de tentativas, fases PREPARING/RENDERING/SAVING e progresso numérico somente quando o Transformer o informa.
- **Concorrência:** instância própria de `ImageAnimationEngine` para cada tentativa, para impedir que a limpeza de um preparo cancelado remova arquivos de outra tentativa. A limpeza durante decodificação é adiada até terminar a operação atual.
- **Rotação:** ViewModel mantém a exportação durante recriação da Activity e as opções visuais são salváveis com valores simples; não recupera trabalho após morte do processo.
- **Compatibilidade:** animação continua offline, PNG temporário sem perdas, H.264 720p/1080p, limite de 80 imagens, formato `schemaVersion=4`, integração com editor e cinco APKs por ABI preservados.
- **Pendente:** build Android Gradle/CI, testes de rotação, cancelamento, salvamento e qualidade MP4 em dispositivo físico. Testes unitários da máquina de estados não provam processamento real.

## v0.1.62 (versionCode 63) · 30/10/2026
- Exportação mais robusta durante mudanças de orientação: estado e progresso agora pertencem ao ViewModel.
- Melhor controle de cancelamento e etapas da exportação; resultados antigos não se misturam com novas tentativas.
- O salvamento final do MP4 na galeria é protegido contra cancelamentos que interrompam a cópia.
- Mantidos 720p/1080p, qualidade solicitada, exportação offline e compatibilidade dos projetos.
- Build Android completo e teste em aparelho permanecem pendentes.

## v0.1.61 (versionCode 62) · 30/10/2026
- **Refatoração incremental de arquitetura:** `EditorProjectViewModel` passa a manter a lista de projetos, o projeto ativo e a ID de sessão durante a recriação da interface. A `MainActivity` apenas consome snapshots e encaminha comandos.
- **Restauração limitada e segura:** `SavedStateHandle` recebe somente o ID do projeto. Ao recriar o ViewModel após perda do processo, a versão atual do projeto é carregada novamente de `ProjectStore`. Não serializa JSON, mídia, bitmaps ou jobs no Bundle.
- **Operações testáveis:** `ProjectSessionCoordinator` encapsula carregar, salvar, abrir, excluir e anexar clipes. Snapshots são publicados apenas depois que o repositório conclui a operação. Projeto excluído/inexistente volta a ID vazia, sem criar dados fictícios.
- **Compatibilidade:** `schemaVersion=4`, regras PiP, legendas, importação cancelável, qualidade H.264/AAC da 0.1.56 e cinco APKs por ABI preservados.
- **Validação:** testes isolados do coordenador e análise estática/sintática; build Android completo, lint Gradle e testes em aparelho ainda exigem CI. Não afirmar retomada de importação/exportação após morte de processo.

## v0.1.60 (versionCode 61) · 30/10/2026
- **Refatoração do ciclo de vida das importações:** novo `MediaTaskCoordinator`, independente de Activity/Context, que coordena vídeos, áudio e PiP usando o escopo Compose existente. Encerramento da composição cancela a leitura pendente; não é um serviço em segundo plano nem um ViewModel persistente.
- **Cancelar importação:** diálogo da leitura de metadados oferece cancelamento, liberando a interface imediatamente. As leituras no Android continuam cooperativas: operações bloqueantes do sistema podem terminar posteriormente, mas seu resultado cancelado não deve ser aplicado.
- **Troca de destino segura:** ao mudar projeto, tela ou ferramenta, a tarefa antiga é cancelada. Tickets únicos impedem que um `finally` de tarefa cancelada libere outra importação iniciada em seguida, inclusive da mesma categoria.
- **Tratamento de falhas:** cancelamentos não são reportados como erros; falhas reais mantêm diagnóstico e mensagem genérica. As escritas do projeto continuam no dispatcher principal após a leitura de metadados em IO.
- **Compatibilidade:** schemaVersion 4, projetos, Media3 e qualidade solicitada de exportação (720p/8 Mb/s; 1080p/18 Mb/s) inalterados.
- **Verificações:** cinco testes Kotlin/JVM isolados da camada de tarefas passaram, incluindo cancelamento, reentrada e erro real. Testes Gradle/Android completos e validação em aparelho ainda pendentes.

## v0.1.59 (versionCode 60) · 30/10/2026
- **Refatoração de tarefas de mídia:** `MediaImportTasks.kt` concentra as consultas a metadados de vídeo, áudio e PiP em `Dispatchers.IO`; evita que os seletores SAF bloqueiem a thread principal com leitura de duração e nomes.
- **Concorrência e destino:** `MediaTaskGate` permite apenas uma importação ativa; `MediaTaskOrigin` rejeita resultados quando projeto, tela ou ferramenta já mudaram. Áudio e PiP conferem ainda o projeto em memória antes de salvar.
- **Importação inválida:** o PiP não recebe duração fictícia quando os metadados não puderem ser verificados, e não é inserido quando a posição não tem espaço restante no vídeo.
- **Interface mais enxuta:** removidos imports obsoletos de `MainActivity.kt`; o estado de importação apresenta diálogo único e continua restrito ao escopo de corrotinas do Compose.
- **Compatibilidade:** sem mudanças em `schemaVersion=4`, projetos, formatos de mídia, permissões, exportadores ou opções de 720p/1080p e bitrate da v0.1.56.
- **Validação:** testes unitários novos para exclusão mútua e rejeição de resultados tardios, compilação Kotlin/JVM isolada incluindo o adaptador com corrotinas, validação estrutural e integridade do ZIP. Gradle/Android completo, regressão de UI e qualidade real do MP4 continuam pendentes.

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
- Padronização dos ajustes de Animar Imagens e botão Salvar edição no cabeçalho.
- Rascunho local persistente (fotos, sequência e opções) com confirmação ao sair e recuperação ao reabrir.
- Confirmação de vídeo salvo compartilhada com a exportação do editor: Abrir, Compartilhar, Editar na timeline, Fechar.
- O editor continua sendo o local das ferramentas de música, áudio, legendas, PiP e cortes, após a geração do MP4.
- Corrigido bloqueio de Voltar depois de salvar; a finalização MediaStore permanece protegida.
- Código-fonte validado estruturalmente; compilação Android e teste real no dispositivo pendentes.

## v0.1.51 (versionCode 52) · 30/10/2026
- Animar Imagens: corrigido o NPE do `LinearProgressIndicator` removendo `progress!!` e usando percentual imutável limitado a 0–100%, barra indeterminada quando ausente.
- Prévia com área compacta de 238 dp e proporção preservada, cartões menores, nomes curtos e controles **Ajustes/Animar** lado a lado.
- Alça de arrastar reposiciona a foto durante o gesto e permite rolagem nas bordas, mantendo botões subir/descer/remover e ordem da galeria.
- Identidade Uai Cortes no progresso, mantendo Media3 apenas no código e nos diagnósticos.
- Novo teste unitário do progresso; compilação Android, exportação MP4 e testes de gesto no aparelho ainda pendentes.

## v0.1.50 (versionCode 51) · 30/10/2026
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

## v0.1.36 (versionCode 37) · 20/10/2026
- Correção de compilação do workflow "Gerar APK Uai Cortes 31": `searchQuery` era acessada no callback de importação SRT antes de ser declarada em `SubtitleScreen`; a declaração do estado foi movida para antes do importador.
- Adicionada verificação estrutural específica para evitar regressão de escopo/ordem no importador SRT. Preservados busca, legendas, prévia adaptativa, exportação e `schemaVersion=4`.
- Logs confirmaram falha em `:app:compileDebugKotlin` e `:app:compileReleaseKotlin` na linha 2926, após a validação estrutural aprovada. Novo build Android, testes e APKs pendentes de execução no GitHub Actions.

## v0.1.35 (versionCode 36) · 20/10/2026
- Prévia Media3 com dimensões proporcionais ao vídeo em Legendas, Cortar e unir, PiP e Exportar. A altura usa limite compatível com a tela, sem barras pretas externas artificiais ou esticar o arquivo.
- Editor principal, PiP e Exportar acompanham a mudança de proporção entre clipes durante a reprodução.
- Legendas: quebra de palavra mitigada medindo a maior palavra e ajustando fonte apenas na prévia; largura e posição do bloco respeitam os limites do quadro real.
- Conteúdo e estilo persistido da legenda não se alteram; schemaVersion 4, comportamento do exportador, SRT e mídia originais preservados.
- Adicionado `AdaptiveVideoLayout.kt` e testes de dimensões/aspect ratio em `AdaptiveVideoLayoutTest.kt`. Compilação Android e verificação de fidelidade no MP4 ainda pendentes.

## v0.1.34 (versionCode 35) · 20/10/2026
- Legendas: busca rápida de texto independente de maiúsculas e acentos, contagem do resultado e indicação clara de busca sem correspondências. A numeração permanece a da lista completa.
- Prévia fixa ganhou botões de navegação para legenda anterior / seguinte, na ordem temporal, com posição do vídeo sincronizada e sem salto circular involuntário.
- Excluir legenda requer confirmação; Cancelar mantém legenda, texto e horários. Após excluir, a seleção avança para um item existente.
- Nova legenda respeita duração do vídeo mesmo no último milissegundo; caso vídeo esteja vazio, o botão de criação é desabilitado.
- Importador SRT rejeita arquivos acima de 250 mil caracteres, evitando importação silenciosamente incompleta.
- Adicionados helper puro `SubtitleListNavigation.kt`, testes JUnit, checks estruturais e documentação. `schemaVersion=4` mantido; compilação Android e testes físicos pendentes.

## v0.1.33 (versionCode 34) · 20/10/2026
- Legendas: prévia do vídeo fixa no topo, com reprodução Media3 restrita ao trecho selecionado e posicionamento por arrasto no próprio quadro.
- Timeline única, horizontal e virtualizada por blocos de 10 segundos. Indicadores utilizam intervalos absolutos, inclusive quando legendas se sobrepõem.
- Lista compacta LazyColumn: só uma legenda expande por vez, com texto, tempos numéricos e ajuste de alças; estilos e posição avançada recolhidos inicialmente.
- Importar SRT, prévia/exportação, IDs e schema 4 continuam compatíveis. Acrescentados testes de janelas temporais.
- Verificações locais de sintaxe/estrutura e lógica; build Android, lint e testes visuais em aparelho pendentes.

## v0.1.32 (versionCode 33) · 20/10/2026
- Cortar e unir: faixa horizontal de trechos com uma miniatura carregada sob demanda por clipe, parte numerada, horários e borda azul na seleção. Toque no cartão para selecionar e rolagem automática até a parte ativa.
- Dividir agora seleciona automaticamente a parte à direita, posicionando o cursor na divisão em milissegundos da fonte. O ponto e ID do trecho ficam gravados nos dados opcionais do projeto, restaurados ao reabrir, sem mudar `schemaVersion=4`.
- Excluir escolhe o trecho vizinho; Desfazer restaura também os dados do último corte; ao dividir, as transições já presentes na fronteira original são preservadas na nova fronteira externa.
- Testes JUnit `TrimTimelineEditTest` para sequência de cortes, seleção, exclusão, transições, JSON legado e pontos inválidos. Compilação Android e inspeção visual em celular continuam pendentes.
- Gradle, workflow, script de cinco APKs, verificador estrutural e tela Sobre sincronizados em 0.1.32 / 33.

## v0.1.31 (versionCode 32) · 20/10/2026
- Ação “Salvar e voltar à Home” retirada de acima das trilhas e convertida em cartão **Salvar** no carrossel de ferramentas do editor.
- Barra inferior com rolagem horizontal cíclica, acessível à esquerda e à direita. Cartões de Cortar, Unir, PiP, Áudio, Transição e Legendas preservados.
- Em Cortar e unir, **Salvar** aparece após Excluir na faixa horizontal rolável, persistindo o corte pendente quando válido e retornando ao início. Erros de intervalo não fazem perder a edição.
- `TrimSaveLogic` criada para tornar a aplicação do corte testável; adicionado `TrimSaveLogicTest` com cenários válidos, inválidos e inalterados.
- `versionName=0.1.31` e `versionCode=32` alinhados ao workflow, script de empacotamento, Sobre e verificador estrutural.
- Sem exportação automática. Gradle/Android CI e validação real de interface ainda pendentes.

## v0.1.30 (versionCode 31) · 19/10/2026
- Correção do build 26: `PathJoin` (símbolo inexistente no Compose usado pelo projeto) substituído por `StrokeJoin.Round` no contorno das legendas, mantendo a mesma intenção visual.
- Preservados os controles de estilo, sombra, entrelinhas e alinhamento da v0.1.29.
- Cinco variantes Release (arm32, arm64, x86, x86_64 e universal) permanecem configuradas para GitHub Releases.
- Compilação Kotlin, lint, empacotamento e publicação da v0.1.30 ainda dependem de nova execução do workflow.

## v0.1.29 (versionCode 30) · 19/10/2026
- Novos controles de sombra deslocada (0–6 dp, preta/branca), espaçamento entre linhas (100–160%) e alinhamento à esquerda, centro ou direita.
- Legendas antigas mantêm sombra desligada, espaçamento padrão e alinhamento centralizado.
- Prévia e Media3 recebem os novos efeitos, com necessidade de teste real em Android e comparação com vídeo exportado para comprovar fidelidade.
- Cinco APKs Release por ABI + universal continuam configurados para GitHub Releases; execução de CI pendente.

## v0.1.28 (versionCode 29) · 19/10/2026
- Editor de legendas: fonte Moderna, Serifada ou Monoespaçada e contorno ajustável (0–4 dp), preto ou branco.
- Prévia Compose atualizada com traço por baixo do preenchimento e renderização Media3 estendida com `TypefaceSpan` e `LineBackgroundSpan` para contorno por linha.
- Persistência individual em `SubtitleCue`, leitura tolerante a projetos legados e cópia de aparência para todas as legendas sem modificar intervalos e conteúdo.
- Novo `SubtitleTypographyTest`; testes Kotlin/Gradle e comparação visual com MP4 no aparelho ainda pendentes.

## v0.1.27 (versionCode 28) · 19/10/2026
- Adicionados cinco estilos de legenda: Clássico, Destaque, Ciano, Limpo e Etiqueta. A escolha altera apenas a aparência da legenda selecionada.
- Seletor de fundo (sem fundo, suave, escuro ou claro) e controle de negrito, com atualização imediata das prévias no editor e sobre o quadro do vídeo.
- Exportação Media3 usa a cor de fundo individual e a opção de negrito via `SpannableString`/`TextOverlay`, sem alterar tempos, IDs, X/Y ou o arquivo original.
- Botão de copiar aparência para todas as legendas, com confirmação e preservação de texto, posição e intervalos.
- Modelo `SubtitleCue` inclui `backgroundColor` e `bold` opcionais na leitura de JSON, mantendo o fundo escuro e o negrito de projetos anteriores. Schema continua v4.
- Novo `SubtitleStylesTest` com verificações de presets, cópia e leitura JSON legada. Build e renderização real no Android pendentes.

## v0.1.26 (versionCode 27) · 19/10/2026
- Tela principal: blocos individuais na pista de legendas abrem diretamente a legenda selecionada por ID.
- Tela Legendas: faixa individual por texto, inclusive com sobreposição temporal; alça esquerda ajusta início, centro desloca mantendo duração e alça direita ajusta fim.
- Rascunho visual durante o arrasto e persistência apenas ao soltar, preservando texto, X/Y, estilo e JSON legado.
- Limites de projeto e proteção de duração mínima de 100 ms para gestos em trechos normais, respeitando legendas SRT antigas menores que isso.
- Adicionados `SubtitleTimelineEdit` e testes unitários de início, fim, deslocamento, overflow e trechos curtos. Build Android e testes físicos pendentes.

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

## Correção v0.1.22 (versionCode 23)
- Corrige o erro `No value passed for parameter onClip` em `MainActivity.kt:825` encontrado no build 18.
- Compilação Android e teste físico ainda pendentes.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

# Uai Cortes v0.1.22 · versionCode 23

## Correção do build 16
- Resolve uso de API `UnstableApi` do Media3 dentro das lambdas `AndroidView` que fazia `:app:lintDebug` falhar. Configurações de vídeo centralizadas em função com `@androidx.annotation.OptIn`.
- Mantém editor, quatro PiPs, legendas, cortes, áudio, fades e efeitos já implementados.

## Um Release com cinco APKs para baixar
- `UaiCortes-v0.1.22-arm32.apk` (ARM 32-bit, `armeabi-v7a`)
- `UaiCortes-v0.1.22-arm64.apk` (ARM 64-bit, `arm64-v8a`)
- `UaiCortes-v0.1.22-x86.apk` (Intel x86 32-bit)
- `UaiCortes-v0.1.22-x86_64.apk` (Intel x86 64-bit)
- `UaiCortes-v0.1.22-universal.apk` (universal, indicado quando não souber a ABI)

O CI publica **somente os cinco APKs Release** como arquivos diretos em GitHub Releases, não repete o pacote no Actions Artifacts e não reexecuta automaticamente por causa de tag criada pelo Release.
Todas as variantes usam o mesmo versionCode e a mesma assinatura do workflow. Utilize apenas uma por instalação.

## Limitações e validação
- Android Lint e compilação completa ainda precisam ser confirmados no próximo GitHub Actions.
- As variantes por ABI podem ser semelhantes em tamanho quando o aplicativo não embarca bibliotecas nativas específicas.
- Fades, exportação de múltiplos PiPs, SRT e extração AAC ainda precisam de validação em aparelhos reais. Dissolvência, MP3 e WAV não estão disponíveis.
