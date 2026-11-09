# Uai Cortes · v0.1.266 — quadro sem timeout e menu do clipe por toque

- Captura de quadro: vídeo sem PiP, legenda ou marca d'água ativa é extraído diretamente do arquivo original, preservando a resolução e evitando o timeout `ERROR_CODE_MUXING_TIMEOUT` observado com a exportação temporária de 260 ms no aparelho.
- Quadros com camadas continuam usando renderização Media3, com trecho mais longo e sem áudio; caso falhe, o usuário pode optar por salvar apenas o original, com aviso explícito sobre a ausência das camadas.
- Toque no clipe da linha do tempo abre menu com Cortar, Editar e Excluir, sem precisar do botão Editar separado. Pressionar/arrastar mantém a reordenação; alças continuam ajustando início/fim.
- Correção de código e testes locais não substituem compilação e teste real no Android, especialmente na composição com PiP/legendas.

# Uai Cortes · v0.1.265 — quadro renderizado sem bordas da prévia

- O botão de câmera deixa de copiar os pixels da janela com PixelCopy; cria um trecho temporário de vídeo pelo compositor Media3 e extrai o quadro já composto em resolução de origem até 1080p (lado menor).
- Captura camadas de PiP, legendas e marca d’água com seus tempos relativos, sem incluir margens pretas ou barras da interface; não é um simples upscale da prévia.
- Para transições que exigem dois clipes simultâneos, mostra aviso e pede avançar alguns quadros em vez de criar imagem incoerente. Saída PNG local na galeria; o MP4 temporário é removido.
- Projetos, originais, editor de áudio, TTS e integrações preservados. Compilação Android e comparação visual no aparelho ainda são necessárias.

# Uai Cortes · v0.1.264 — CI 221: correção de compilação dos pop-ups

- Corrigida a ausência de `import androidx.compose.material3.Surface` em `ProjectDraftUi.kt`, que fazia o workflow 221 falhar em `:app:compileDebugKotlin`.
- Adicionado teste de regressão para impedir que os diálogos usem `Surface` sem o import do Compose Material 3.
- Preservados Editor de Áudio, importação de som de vídeos, cursor na onda sonora, recuperação de rascunhos, dados, assinatura e workflow de cinco APKs.
- Testes estáticos e código-fonte precisam de confirmação pela compilação Android do próximo GitHub Actions.

# Uai Cortes · v0.1.263 — cursor na onda sonora e edição direta do áudio de vídeo

- Teste legado do atualizador sincronizado com “Reiniciar download”, preservando o controle de pausa e a validação SHA-256.
- Editor de Áudio: cursor de reprodução se desloca visualmente na onda sonora usando posição real do player, com busca por toque respeitando o intervalo escolhido e alças independentes.
- Nova entrada **De vídeo** permite escolher um vídeo pelo seletor Android e editar diretamente sua faixa sonora, sem gerar arquivo temporário adicional; a saída continua M4A/AAC com somente áudio.
- Vídeos sem áudio compatível apresentam mensagem própria. MP3, WAV e M4A já aceitos continuam funcionando; fontes originais são preservadas.
- Compatibilidade dos codecs e fidelidade de cursores/corte dependem de validação com APK e testes reais no Android; sintaxe e testes locais não substituem a compilação completa.

# Uai Cortes · v0.1.262 — diálogos mais claros e atualização sem botão quebrado

- Pop-ups principais da Home e de Atualizações receberam hierarquia visual melhor, com ícones, mensagens de apoio e destaque para informações temporárias ou seguras.
- O diálogo de Novo projeto ficou mais direto, com explicação curta e campo com orientação; Recuperar edição e Atualização disponível também foram reorganizados.
- Na tela Atualizações, os controles de Pausar, Parar e Reiniciar download se adaptam a telas estreitas para impedir que o texto invada o retângulo do botão.
- Alteração visual e de layout validada localmente; compilação Android completa e teste físico ainda precisam ser confirmados no CI/aparelho.

# Uai Cortes · v0.1.261 — CI 218, quadro visível e reprodução única

- Atualizado o contrato de navegação para reconhecer o novo Editar áudio sem perder Dividir vídeo/Extrair áudio.
- Botão de câmera salva a composição visível no editor via PixelCopy: vídeo, legendas, PiP e efeitos da prévia. A imagem reflete a resolução exibida na tela, não necessariamente a origem.
- Em Cortar vídeo, a reprodução para ao fim do trecho selecionado e só recomeça após novo toque. Fonte original, recortes e exportação permanecem intactos.
- CI e workflow ainda devem comprovar build Android; PixelCopy e saída PNG precisam de teste no aparelho.

# Uai Cortes · v0.1.260 — Editor de Áudio independente

- Novo cartão Editar áudio na faixa de atalhos da Home, com seleção de arquivo via Android, forma de onda real em segundo plano quando suportada e recorte com duas alças.
- Prévia somente do trecho selecionado com repetição opcional, ajuste fino de 0,1 s e exportação AAC em M4A no local escolhido via SAF. Arquivo original preservado.
- Sem servidores ou chamadas à internet; duração de áudio e codecs dependem do aparelho. Build Android e validação física pendentes.

# Uai Cortes · v0.1.259 — Estúdio de Voz reformulado

- Tela Texto para Voz reorganizada como Estúdio de Voz: editor compacto, estimativa de duração, botão de colar, voz e apelido visíveis, estilos rápidos, idioma pt-BR/automático, opções técnicas recolhidas e botão de geração fixo acima da área de navegação/teclado.
- Biblioteca de WAVs agora aceita renomeação, favoritos e metadados opcionais de voz/duração/trecho para novos áudios, preservando gravações antigas. Reprodução, exportação SAF e exclusão com confirmação mantidas.
- Rascunho local gravado apenas a pedido, com restaurar e apagar. Adição confirmada à timeline do projeto aberto copia o WAV para mídia protegida do projeto, sem depender da biblioteca removível.
- Continua sendo necessária a API Gemini para novas sínteses; nenhuma geração automática nem servidor adicional. A integração Android completa depende de GitHub Actions e testes físicos, não comprovados só pelos testes locais.
- ZIP oficial inclui workflow `.github/workflows/android-apk.yml` na versão 0.1.259/versionCode 260.

# Uai Cortes · v0.1.258 — Correção de compilação da Central de Vozes

- Corrige o CI 215: `GeminiVoicePaging.range()` retorna `Pair<Int, Int>`; a Central agora mostra o último índice usando `second`, não `last`.
- Mantidas as 500 vozes salvas, paginação por 50, filtros, favoritos, históricos WAV, autorizações e todo o sistema de edição e redes sociais.
- Teste Kotlin isolado e contrato da tela acrescentados; build completo e geração dos cinco APKs ainda precisam de confirmação pelo GitHub Actions.
- ZIP de fonte oficial contém `.github/workflows/android-apk.yml` sincronizado.

# Uai Cortes · v0.1.257 — Central de Vozes paginada

- O catálogo Gemini pode salvar até 500 vozes. Exibição: 50 por página, mantendo busca e filtros globais sem rede por página.
- Filtros rápidos visíveis; ações de renomeação, favoritos, prévia e detalhes alinhadas; comparação A/B mantida.
- Índice de pesquisa e resultados memorizados, digitação com debounce de 180 ms, métricas de desempenho locais opcionais sem registrar textos ou conteúdo de áudio.
- O cache WAV e as vozes salvas não são removidos. Síntese de novas prévias continua sujeita à confirmação e cotas da API.
- A compilação Android completa ainda precisa ser validada pelo CI; o ZIP traz `.github/workflows/android-apk.yml`.

# Uai Cortes · v0.1.256 — Conexão YouTube simplificada

- Ao atualizar vídeos do YouTube de um canal antigo, o aplicativo identifica a conta Google escolhida e a associa ao canal apenas após verificar o ID pela API. Na atualização seguinte, tenta recuperar a autorização sem repetir a escolha.
- Novos canais vinculados guardam automaticamente o e-mail verificado, mas nunca tokens OAuth. Você não precisa digitar seu e-mail para cada canal.
- Se o Google revogar ou não conceder uma permissão, será necessário autorizar novamente. Consultas são explícitas; publicação ainda exige confirmação e escopo adicional.
- Corrigido `horizontalScroll` em Texto para Voz, responsável pelo erro de compilação do workflow 213.

# Uai Cortes · v0.1.255 — Vozes Google com renomeação e português BR

- Central de Vozes: toque no lápis ao lado de uma voz para renomeá-la localmente; o identificador da voz Google não muda.
- A geração Gemini TTS recebe orientação de fala pt-BR por padrão. Prévia e Texto para Voz permitem selecionar detecção automática. Testar pronúncia e sotaque em cada voz; não há garantia de sotaque brasileiro perfeito.
- As prévias históricas sem orientação explícita permanecem acessíveis offline; novas amostras usam versão nova de cache.

# Uai Cortes · v0.1.254 — Canais Google com conta associada

A atualização dos vídeos do YouTube usa a conta Google vinculada ao ID do canal, sem escolher novamente cada conta toda vez. Em Ajustes → Minhas contas e canais → Conta Google, informe uma vez o e-mail, confirme a autorização e o app valida o canal pela API antes de gravar a associação. Não persiste tokens OAuth. A leitura usa youtube.readonly, e o upload pede youtube.upload apenas durante publicação confirmada. Sem vínculo, não consulta a conta errada. Consentimento Google pode aparecer se necessário.

# Uai Cortes · v0.1.253 — Correção do pacote de código-fonte do CI 210

O workflow 210 falhou antes da compilação porque a versão do workflow no GitHub ficou em 0.1.251 enquanto o código Android estava em 0.1.252. O ZIP de fonte anterior omitiu o diretório oculto `.github/`. Este pacote inclui o workflow atualizado e um teste que valida **o ZIP real extraído**, não só a pasta de desenvolvimento.

**Para importar:** manter todos os arquivos e diretórios do ZIP, **inclusive `.github/workflows/android-apk.yml`**, ao enviar ao repositório. Gerar o pacote apenas com `python3 tools/package_source.py --output /caminho/arquivo.zip`. Verificar a alteração do workflow antes de executar novamente o GitHub Actions.

# Uai Cortes · v0.1.252 — Correção da compilação CI 209

O workflow **209** passou pelos testes Python e chegou ao Gradle, mas `:app:compileDebugKotlin` falhou em `ExportScreen.kt:501:36` por `Unresolved reference 'InfoOutline'`. A referência foi trocada por `Icons.Default.Info`, já utilizada em outras telas do aplicativo, sem alterar o texto, posicionamento ou comportamento da confirmação de exportação.

Adicionado teste `tools/test_ci209_export_info_icon_contract.py` para rejeitar o símbolo inválido e manter o aviso de confirmação. O projeto preserva todos os recursos anteriores, incluindo YouTube/Facebook Páginas, Coqui/Chatterbox, Gemini, legendas, cinco APKs previstos e assinatura. O build Android ainda deve ser confirmado no GitHub Actions.

# Uai Cortes · v0.1.251 — Correção do CI 208

O workflow 208 parou na validação Python porque um teste antigo ainda esperava o botão **Novidades** dentro das Configurações e histórico na tela pós-atualização. Esses itens foram intencionalmente removidos na v0.1.248. A versão 0.1.251 ajusta os testes ao comportamento atual sem regredir a interface, conservando o catálogo interno de histórico e a abertura automática de Novidades após atualizações. Mantém as funcionalidades anteriores de publicação, redes, vozes e exportação. Build Android e testes no aparelho seguem pendentes.

# Uai Cortes · v0.1.250 — Redes com consulta de publicações Facebook

- Redes Sociais passa a alternar entre vídeos do YouTube e publicações Facebook de Páginas autorizadas. A leitura Facebook acontece apenas quando o usuário solicita; filtros por Página e pesquisa local sobre o que foi carregado.
- A Meta pode não devolver todos os Reels e pode omitir miniaturas/visualizações: o aplicativo informa a limitação, sem fabricar estatísticas ou realizar publicações.
- Tokens OAuth/Meta não são persistidos; dados de projetos, edições e vídeos enviados permanecem no mesmo esquema.
- A consulta depende de configuração Meta, permissões aprovadas e conexão. Necessita testes reais no aparelho, além da validação do código.


A aba Redes Sociais agora destaca **Publicar vídeo** num cartão principal, seguido de Visão geral com filtros por canal e estatísticas realmente recebidas do YouTube. Os vídeos continuam em duas colunas com miniatura 16:9, título, origem e visualizações. As mensagens de ausência de canais/vídeos e a busca em janela central foram reorganizadas. Tudo respeita temas claro, escuro e AMOLED, sem criar solicitações de rede ao digitar. Não alteramos OAuth, armazenamento de projetos, esquema social, atualização dos canais ou envio de vídeos. Facebook/Reels não são listados ainda, pois faltam integração e autorização específicas de consulta. A validação visual e o build Android completos seguem pendentes.

# Uai Cortes · v0.1.248 — Biblioteca de vídeos e publicação renovadas

Na aba Redes Sociais, os **Vídeos publicados** de canais YouTube consultados aparecem em dois cartões por linha com imagem de capa real, contador de visualizações e indicação de canal. A lupa abre a busca local centralizada por título/canal; atualizar é uma ação manual única. Imagens de URL restrita (YouTube) são buscadas sem token OAuth e guardadas somente em cache de memória limitado. Miniaturas ainda podem faltar por falta de rede, remoção do vídeo ou CDN indisponível.

**Minhas contas e canais** foi movido para Configurações, conservando cadastro, vinculação e autorização de canais YouTube e Facebook Página. A biblioteca principal ainda consulta somente vídeos do YouTube; Facebook/Reels na listagem requer API e autorização próprias no futuro. Publicar vídeo aceita um arquivo por vez, mostra miniatura local e nome, permite assistir e remover com X antes de escolher outro, mantendo vários destinos quando permitidos e confirmação antes do upload.

O diálogo de exportação e o de autorização do YouTube usam o novo componente visual reutilizável com ações claras. A tela Novidades após atualização mostra apenas as alterações atuais; histórico técnico permanece na documentação e catálogo interno, mas não abre na interface pós-atualização. Não modificar apps/credenciais nem enviar vídeos sem toque explícito. Compilar CI e conferir UI no aparelho antes de usar como APK estável.

# Uai Cortes · v0.1.247 — Vozes Google com histórico de prévias offline

A Central de Vozes Gemini ganhou acesso a amostras WAV anteriores, mesmo depois de trocar a frase de comparação, o modelo ou o estilo. Na ficha de uma voz, use **Prévias offline** para listar o histórico local conhecido, reproduzir, parar, salvar WAV via seletor de arquivos do Android ou excluir só a amostra escolhida. O catálogo, os favoritos, as narrações e os projetos não são alterados.

O filtro **Somente prévias da frase/modelo atuais** continua existindo, separado do novo filtro **Vozes com qualquer prévia no histórico offline**. As prévias do histórico são identificadas por voz e hash; modelo, frase e estilo de arquivos antigos **não são recuperáveis**, portanto não são inventados. Prévia anterior ao índice de associação não recebe vínculo presumido.

A síntese Gemini mantém confirmação explícita de envio e possível custo; ações offline não geram requisições. Para impedir confusão no cache, a geração congela modelo, texto e instrução de estilo no início da chamada. Arquivos WAV exportados são salvos somente no destino escolhido, podendo ser usados em editores externos conforme termos dos serviços. Testes Android reais e CI ainda devem confirmar funcionamento.

# Uai Cortes · v0.1.246 — Fotos e projetos com mídia ausente

A importação de fotos agora tenta reconhecer o formato pelos dados decodificados quando a galeria retorna MIME genérico ou não fornece metadados. A validação ainda limita imagens a 24 MiB e 24 megapixels, rejeita formatos não suportados e cancela cópias de forma segura ao sair da tela. Nenhum original é apagado.

Quando um projeto encontra fontes inacessíveis, é apresentado um aviso com a opção **Reparar mídias** que abre o assistente já existente. Ele permite localizar novamente arquivos e preservar os ajustes existentes. A recuperação de fotos descarta a cópia privada provisória se não conseguir salvar o projeto. A checagem de acesso também aceita provedores que permitem abrir uma mídia por stream, mesmo sem descritor de arquivo.

O relatório diagnóstico opcional inclui a contagem (não a identidade) das mídias inacessíveis. Mensagens de importação evitam detalhes privados de provedores externos. Esta é uma versão de código-fonte; validar no APK real os provedores de galeria, o acesso após reiniciar e a reprodução da timeline. As diferenças visuais de legendas na exportação ainda precisam de comparação física de quadros.

# Uai Cortes · v0.1.245 — Atualizações, legendas e exportação

A tela Atualizações agora oferece **Pausar / Retomar / Parar** o download do APK. Retomar usa Range somente quando o servidor retorna intervalo correto; em outro caso reinicia sem reaproveitar um arquivo incompatível. A instalação continua dependendo de verificação SHA-256 e consentimento do Android.

Na edição das legendas, prévia e Media3 compartilham uma escala proporcional à largura do quadro (régua de 280, área horizontal de 90%). Isso reduz divergências anteriores de tamanho, mas requer comparação visual em MP4 exportado, especialmente em 720p/1080p e com o karaokê rosa.

A exportação pede confirmação, mostra o nome sem `.mp4` no campo, adiciona a extensão ao gerar o arquivo e renova as telas de progresso e vídeo salvo. A ferramenta Descrição saiu da barra do editor, mantendo geração de texto/IA na tela final. A qualidade padrão de Geral continua sendo a seleção inicial (pode ser substituída na exportação), com explicação corrigida e detalhes da nova mídia privada.

O diagnóstico opcional registra estados de download e exportação, classifica motivos de encerramento quando o Android fornece essa informação e limita taps redundantes. Os logs não capturam conteúdo privado. Esta é uma versão-fonte 0.1.245/versionCode 246; não é garantia de compilação nem de paridade visual sem CI/testes físicos.

# Uai Cortes · v0.1.244 — Correção Kotlin do karaokê rosa (CI 203)

O teste antigo de Texto para Voz foi ajustado ao atalho `HomeActionTile` da faixa horizontal, sem recolocar cartões duplicados na tela inicial.


O workflow 203 parou na compilação Kotlin (Debug e Release) porque `SubtitleKaraokeBadge.kt` importava `drawBehind` do pacote errado. Agora o efeito usa `androidx.compose.ui.draw.drawBehind`; o desenho `DrawScope.drawRoundRect` permanece inalterado. O teste `tools/test_ci203_karaoke_drawbehind_244_contract.py` previne a regressão.

Esta correção foi aplicada sobre **0.1.243**, preservando os sete atalhos azuis com rolagem horizontal, o renomear de projetos, o karaokê rosa e as vozes Coqui/Chatterbox. É uma atualização de código-fonte para **0.1.244/versionCode 245**; os cinco APKs Release e o comportamento visual no aparelho exigem verificação pelo GitHub Actions e dispositivo Android.

# Uai Cortes · v0.1.243 — Sete atalhos roláveis e renomeação de projetos

A página inicial reúne **Novo projeto**, **Animar fotos**, **Estúdio de Cenas**, **Vozes Google**, **Texto para Voz**, **Extrair áudio** e **Dividir vídeo** em uma única faixa de cartões azuis com rolagem horizontal. Os cartões grandes e botões independentes que ocupavam espaço abaixo foram removidos. Cada atalho mantém seu destino original.

Em **Edições recentes** e na aba **Projetos**, toque no **lápis** ao lado da lixeira para renomear. O nome é validado, limitado a 80 caracteres e confirmado em diálogo. Renomear altera somente o título e a data de atualização no cadastro mais recente; não recria o projeto, não move nem apaga mídias e não altera faixas ou legendas. Falhas de persistência são informadas sem descartar o conteúdo original. Não é possível renomear durante edição com alterações ainda não salvas.

A novidade mantém as legendas karaokê rosa, a biblioteca Coqui/Chatterbox, a exportação e o schema de projetos anterior. Regresso e sintaxe podem ser verificados localmente; compile no GitHub Actions e teste a interface em Android antes de publicar como validada.

# Uai Cortes · v0.1.242 — Karaokê rosa igual ao estilo do vídeo

A galeria **Legendas > Estilo** agora destaca **Karaokê rosa do vídeo** como primeira opção. As letras são brancas e maiúsculas, com contorno preto e a palavra falada destacada por um retângulo rosa arredondado. O fundo do restante da frase permanece transparente. A prévia, a miniatura do estilo e a exportação Media3 usam o mesmo efeito `karaoke_badge`.

Nas **Legendas automáticas**, a aparência inicial sugerida é **Rosa do vídeo** (22 sp, centralizada a cerca de 79% da altura). Você pode escolher **Karaokê IA** ou **Clássica (SRT)**. Novas legendas manuais também usam o efeito rosa; legendas e projetos existentes mantêm seu estilo original até escolha explícita. A sincronia segue os tempos por palavra quando disponíveis; sem eles, o destaque avança por divisão aproximada do intervalo, não por análise do áudio.

A fonte é a sans-serif em negrito do Android, similar, não necessariamente idêntica à usada na referência. Não muda modelos de voz nem serviços externos. A compilação e o vídeo exportado precisam ser validados em aparelho/CI.

# Uai Cortes · v0.1.241 — Biblioteca de vozes clonadas Coqui + Chatterbox

Em **Vozes IA > Clonar voz**, importe uma referência WAV de 3–30 segundos, escolha Coqui XTTS-v2 ou Chatterbox Multilingual V3, fixe-a no personagem e gere novas falas. Os modelos são processados por serviço Python HTTPS externo, com token informado em cada sessão. O app mantém a referência privada e insere WAVs após ação explícita, sem regenerar por Gemini. O backend e as instruções estão em `tools/voice_bridge/README.md`.

> É necessário configurar e executar o serviço Python: o APK não inclui modelos ou clonagem local Android. Coqui XTTS-v2 pode restringir o uso comercial dos áudios. Não clonar vozes sem autorização. O workflow 201 interrompeu na regressão legada da interface (4 ramos de botões / contador de áudios); os contratos foram corrigidos.

# Uai Cortes · v0.1.240 — Vozes IA: personagens, humor e importação externa

A tela **Vozes IA** respeita agora a voz fixa de cada personagem nas gerações em lote; os humores individuais continuam independentes. Com personagens definidos, a barra principal mostra Personagens e a lista de falas não permite trocar a voz individualmente. Toque numa fala para abrir ações, incluindo escolher humor ou importar áudio externo (WAV, MP3, M4A, AAC e OGG até 64 MiB). O áudio importado é guardado privadamente, validado e associado apenas à fala escolhida, sem gastar cota Gemini; o WAV anterior não é apagado automaticamente. Lembretes de pendência têm X e progresso separado. Dados de projeto schemaVersion 4, integração Gemini, Polly, Redes e cinco APKs preservados. Compilação Gradle Android e testes físicos continuam pendentes.

# Uai Cortes · v0.1.239 — Backups privados na importação e correção CI199

As cópias de segurança de vídeos, áudios e imagens importados para projetos agora ficam em `filesDir/project_media_v1/<ID>/` e são acessadas por FileProvider, sem registrar novos itens no MediaStore e sem duplicar vídeos na Galeria. A cópia permanece integral, verificada e atômica. Os vídeos finais exportados continuam sendo salvos normalmente na galeria.

Projetos antigos continuam usando as URIs previamente salvas, inclusive os arquivos públicos de `Filmes/UaiCortes/Projetos/`. Nenhuma mídia legada é excluída nem migrada automaticamente. **A mudança evita novas duplicações na galeria, mas não recupera espaço ocupado por cópias já criadas.** Limpar dados ou desinstalar o aplicativo remove arquivos privados e pode deixar projetos sem suas mídias; preservar backup/exportações antes dessas ações.

Corrigida a falha do workflow 199 (`SocialYouTubeLibraryAggregateTest.loadedChannelCountConsidersOnlyNonEmptyLists`): a contagem local de canais com vídeos não inclui listas vazias, enquanto as métricas oficiais dos canais permanecem independentes. Versão 0.1.239, versionCode 240. A compilação Android e testes reais de recuperação/reprodução precisam passar no CI/aparelho.

# Uai Cortes · v0.1.238 — painel Redes unificado

A tela Redes agora reúne canais em uma faixa rolável (Todos/individual), estatísticas de canais consultadas do YouTube e vídeos anteriores junto dos uploads confirmados pelo Uai Cortes. O cartão Desempenho vazio foi removido; a ajuda fica no cabeçalho e o histórico técnico continua disponível recolhido. Consultas exigem ação explícita; a última resposta fica em cache privado local, sem tokens Google nem sincronização em segundo plano. Versão 0.1.238/versionCode 239; build Android e teste real OAuth pendentes.

# Uai Cortes · v0.1.237 — Correção da reprodução no editor

A prévia do editor agora **para ao concluir o último clipe**, em vez de repetir o projeto sem fim. Ao minimizar o aplicativo, bloquear a tela ou abrir outro app, a prévia é **pausada pelo ciclo de vida Android** e não retoma automaticamente quando o usuário volta. Se tocar em Play após o fim, a reprodução começa do início do projeto. Essas mudanças não alteram o vídeo exportado, os áudios originais, nem o posicionamento do projeto. Versão 0.1.237/versionCode 238. APK e teste em aparelho pendentes.

# Uai Cortes · v0.1.236 — Correção CI196 do teste da biblioteca YouTube

A versão 0.1.236 (versionCode 237) atualiza o contrato de regressão `test_social_youtube_history_228_contract.py` para a UI da biblioteca YouTube existente desde 0.1.235: visualizações nos vídeos e agregação dos canais consultados, com paginação individual. O workflow 196 parava na etapa Python antes do build. O comportamento de publicação e as credenciais permanecem preservados. É necessário confirmar a compilação completa pelo GitHub Actions.

# Uai Cortes · v0.1.235 — Redes mais compacta e biblioteca YouTube agregada

A versão 0.1.235 (versionCode 236) reorganiza a aba **Redes Sociais** para economizar espaço, exibindo as contas em grade com dois quadrinhos por linha quando possível. A área **Vídeos do YouTube** agora mostra as visualizações de cada vídeo e junta, em uma única lista e resumo, os dados já carregados de todos os canais consultados na sessão. A consulta continua manual, sem importar vídeos ao histórico do app e sem persistir tokens OAuth. Build Android e teste real no aparelho seguem pendentes.


Na Central de Vozes, abra **A/B** e toque em **Teste cego**. Com pelo menos duas prévias já salvas para a **mesma frase, modelo e estilo**, o app seleciona um par ainda não avaliado quando possível. Toque em 1 e 2 para ouvir offline; os nomes permanecem ocultos até marcar **Parecidas** ou **Diferentes**, ou optar por revelá-los. **Outro par** repete sem custo de API se houver amostras locais. O teste nunca gera áudio automaticamente; novas prévias exigem a confirmação já existente.

As avaliações continuam apenas no dispositivo e são subjetivas. O filtro de ocultar vozes parecidas foi corrigido para respeitar pares marcados explicitamente como diferentes, mesmo em cadeias de similaridade. Vozes de outros idiomas mantêm aviso de pronúncia não garantida em português. Amazon Polly, Gemini, cinco APKs e projetos anteriores permanecem preservados.

# Uai Cortes · v0.1.233 — Comparação A/B e descoberta de vozes Gemini

Na Central de Vozes, selecione A/B diretamente em cada cartão, reproduza o mesmo texto e registre localmente sua impressão de vozes parecidas ou diferentes. Em Filtros, a opção **Ocultar vozes que marquei como parecidas** mantém ao menos uma voz de cada grupo, priorizando favoritas. Isso não é prova de identidade acústica nem aplica classificação automática.

Em Configurações da Central, **Consultar todos os idiomas** busca o catálogo geral Gemini mediante toque, combina novas entradas com o catálogo atual até o limite local de 500 e preserva favoritos, apelidos e prévias; vozes catalogadas em outro idioma podem pronunciar português incorretamente. Só a geração de nova prévia envia texto e requer confirmação. A integração Amazon Polly, seu controle de caracteres e o editor permanecem independentes.

# Uai Cortes · v0.1.232 — Amazon Polly e controle de consumo

Em **Central de APIs e Contas > Amazon Polly**, cadastrar credenciais próprias AWS IAM com permissões `polly:DescribeVoices` e `polly:SynthesizeSpeech`, região da chave e token temporário opcional. O segredo é criptografado no Android Keystore, sem chaves no repositório, e pode ser importado/exportado somente por solicitação, com alerta de risco. Consultar vozes brasileiras não gera áudio; síntese MP3 exige confirmação e não tem retry automático. Limites locais diários e mensais opcionais, histórico CSV somente dos caracteres informados pela AWS e preços públicos estimados, não faturamento real nem saldo AWS. Gemini, Redes e editor permanecem independentes.

# Uai Cortes · v0.1.231 — Central de Vozes organizada e comparação A/B

A Central de Vozes foi compactada para corrigir a área superior em Android edge-to-edge, esconder ajustes infrequentes e expor filtros reais do catálogo Gemini (idioma, sotaque, contexto e persona), sem inferir identidade ou timbre a partir do nome. Apelidos são locais e preservam os IDs. A/B compara duas vozes usando a mesma frase, estilo e modelo, com prévias curtas, padrão ou personalizadas. Só gera uma amostra após confirmação explícita e mantém as anteriores. A seleção segue compartilhada pela Home, Texto para Voz e editor, sem gerar áudio ao selecionar. Compilação Android e teste real com vozes ainda pendentes.

# Uai Cortes · v0.1.230 — Correção de compilação do Texto para Voz

A versão 0.1.230 (versionCode 231) corrige o import `BackHandler` do novo módulo independente Texto para Voz, que ocasionava falha Kotlin no workflow 191. Não altera as chamadas Gemini, a integração do editor, a assinatura Android, a navegação ou o armazenamento de WAV. A compilação Android e o teste real no celular devem ser validados no GitHub Actions e no aparelho.

# Uai Cortes · v0.1.229 — Texto para Voz Gemini independente

A Home agora tem **Texto para Voz**: uma tela separada do editor para digitar/colar texto, selecionar voz na Central de Vozes Gemini, escolher modelo e interpretação, confirmar geração, ouvir, guardar e exportar WAV via Storage Access Framework. Os áudios persistem em armazenamento interno até serem excluídos; cópias exportadas ficam no destino escolhido.

Exibe caracteres, **estimativa aproximada** de tokens do texto (não representa cota/cobrança) e o total efetivamente informado pela API quando disponível. Nenhuma chamada é iniciada pela digitação ou abertura da tela. A chave Gemini configurada no app é reutilizada. Rascunho em memória, sem salvar automaticamente conteúdo de texto. Não integra narrações ao projeto ou ao editor.

# Uai Cortes · v0.1.228 — Consulta de vídeos antigos e Redes compactas

- A aba **Redes** separa **Vídeos do YouTube** (biblioteca remota consultada manualmente) de **Envios pelo Uai Cortes** (histórico local existente). A consulta usa somente o canal selecionado e exige OAuth Google com `youtube.readonly` e confirmação do ID.
- Botão **Buscar vídeos anteriores** carrega 20 itens por página da playlist de uploads do canal; **Carregar mais vídeos antigos** traz páginas adicionais, até 300 itens por consulta de sessão, e o campo Filtrar pesquisa apenas títulos das páginas já carregadas.
- Nada é sincronizado em segundo plano. Resultados remotos são mantidos somente em memória, sem gravar tokens, dados de vídeos de terceiros ou alterar publicações e projetos existentes.
- Filtros do histórico local passaram a caber numa linha rolável. Erros de rede ou acesso não são apresentados como ausência de vídeos.
- A verificação real com dois canais e o build Android completo dependem dos testes GitHub Actions/em aparelho.

# Uai Cortes · v0.1.227 — Redes e publicação compactas

- Redes agora apresenta cada conta em uma linha compacta com nome e tipo; verificar, vincular ou remover aparecem ao expandir. O status de cadastro não afirma login permanente.
- Histórico mantém filtros e avisos importantes, mas recolhe consultas, métricas, Studio, compartilhamento e detalhes, reduzindo o excesso de botões visíveis.
- Refatoração em arquivos dedicados: `SocialScreens.kt` (Redes), `SocialPublishScreen.kt` (publicação), `SocialAccountDialogs.kt` (diálogos), `SocialAccountCard.kt` (conta) e `SocialPublicConfig.kt` (backup público).
- A autorização e o envio continuam exigindo confirmação e canal certo. Nenhum token persistido, sem migração do catálogo social schema 1 ou projetos schema 4, sem reenvio automático.
- Compilação Gradle/Android e testes físicos de acessibilidade, upload e layout continuam pendentes.

# Uai Cortes · v0.1.226 — Histórico de publicação e status do YouTube

## Novidades v0.1.226

- Histórico em Redes com quatro filtros compactos: Todas, Pendentes, Recebidas e Revisar.
- Card do vídeo usa título editado, não nome do arquivo; envios já confirmados não oferecem compartilhar como se fosse obrigatório.
- Consulta voluntária de status oficial YouTube `videos.list(part=status,processingDetails)` após selecionar o canal correto no OAuth. Exibe privacidade e andamento da última consulta, sem declarar vídeo público apenas porque foi recebido.
- Cache local de status público sem tokens, campos opcionais compatíveis com `social_v1` e `schemaVersion=4`; nenhuma chamada automática nem reenvio após upload incerto.
- Build Android, API real, processamento remoto e visual no aparelho ainda pendentes de validação.

# Uai Cortes · v0.1.225 — Publicação após exportação mais direta

- Ao salvar um vídeo, **Publicar vídeo** é a ação principal. Descrição, Gemini e compartilhamento manual continuam disponíveis em opções secundárias.
- A tela **Publicar vídeo** reúne canal, título editável, descrição/hashtags e privacidade. Com um único canal YouTube selecionado, Continuar abre diretamente a confirmação de autorização/envio; vários destinos seguem com confirmação individual.
- O envio distingue progresso ativo de resultado sem confirmação; após resposta da API mostra o ID do vídeo e opção de abrir o Studio. **Recebido não significa público.** Sem reenvio automático de operação incerta.
- O diagnóstico de resolução tenta ler a orientação do arquivo MP4 salvo; sem metadados, mantém a informação do exportador, sinalizando a limitação. Catálogo social schema 1 e projetos schema 4 preservados, inclusive títulos antigos.
- Build Gradle, teste real YouTube e verificação visual em Android permanecem necessários.

# Uai Cortes · v0.1.224 — Confirmação de contas sociais mais simples

- v0.1.224: confirmação de canais YouTube e Páginas Facebook compacta, com dados públicos, botões visíveis e diálogo reutilizado em Redes e Integrações. OAuth, catálogo social, assinatura e exportação preservados.
A aba **Redes > Adicionar** agora apresenta conexão automática de YouTube e Facebook Página. O app consulta o canal ou Página pela API e pede confirmação antes de salvar nome e ID públicos; permite vários canais Google por seleção de conta e evita duplicatas. Destinos sem OAuth validado continuam manuais. Na hora de publicar, o ID do canal é novamente comparado ao ID autorizado. HTTP 403 YouTube informa categoria conhecida de erro sem exibir resposta privada. A API YouTube Data v3 precisa estar ativa no Google Cloud; mudanças no código não resolvem quotas ou permissões bloqueadas. Teste real Android/Meta/OAuth e Gradle pendentes.

# Uai Cortes · v0.1.222 — Correção do build do painel de voz
O workflow 184 identificou que o novo painel inferior em **Editor > Áudio > Melhorar voz** utiliza APIs experimentais do Compose Material 3 sem `@OptIn`. A correção declara `@OptIn(ExperimentalMaterial3Api::class)` no componente afetado, sem modificar efeitos, projetos nem exportação. Incluído teste de regressão CI184; compilação Android final depende de reexecutar o GitHub Actions.

# Uai Cortes · v0.1.221 — Ajustes de voz em pop-ups
Em **Editor > Áudio > Melhorar voz**, o painel mostra só a faixa, o estilo atual, quatro atalhos (Estilo, Ajustes, Equalizador, Limpeza), perfis e comparação Original/Tratada. Os detalhes são editados em painéis inferiores Material 3 com o mesmo tema e formas do Uai Cortes, sem opções cortadas por rolagem horizontal. O processamento PCM, presets, perfis e exportação permanecem inalterados.

# Uai Cortes · v0.1.220 — Tratamento de voz aperfeiçoado

Em **Editor > Áudio > Melhorar voz**, o painel oferece Natural/Encorpada/Clara/Estúdio, intensidade, compressão, compensação de volume, EQ de quatro bandas (±6 dB), de-esser e redução de ruído leve por expander. A/B retoma o mesmo ponto da faixa ao alternar. Três perfis de ajuste podem ser salvos localmente; é possível copiar o ajuste para outros vídeos mediante confirmação. Os efeitos offline são aplicados na exportação Media3 à fonte escolhida, sem alterar os arquivos originais nem automaticamente a música de fundo. Não há RNNoise, separação de fontes, cálculo LUFS ou limitador true peak nesta versão. A qualidade auditiva, a compilação Android e o uso de memória devem ser verificados no aparelho.

# Uai Cortes · v0.1.219 — Melhorar voz offline no editor

Em **Editor > Áudio > Melhorar voz**, selecione o áudio de um vídeo ou uma faixa adicionada. Ative Natural, Encorpada ou Clara e ajuste a intensidade; o **Equalizador (EQ) avançado** permite alterar graves, corpo/médios e clareza (±4 dB). Use **Original** e **Tratada** para comparar a fonte isolada. Os mesmos filtros PCM são usados na exportação Media3, sem modificar a mídia de origem. Projetos anteriores mantêm o efeito desligado. O efeito não isola voz de música nem remove ruído por IA; processamento e testes de nível de saída dependem de avaliação em aparelho. Release 0.1.219, versionCode 220.

# Uai Cortes · v0.1.218 — Correção CI180 (YouTube)
**Atualização v0.1.218 (versionCode 219):** correção do teste estrutural legado do YouTube, que ainda exigia `testBusy` na tela antiga; agora valida o estado real (`youtubeBusy`) do upload e o teste Google OAuth na nova Central de Integrações. Sem alterar o fluxo de publicação, credenciais, interface ou schema 4. Build Gradle e OAuth real pendentes.

# Uai Cortes · v0.1.217 — Integrações e redes sociais

**Atualização v0.1.217 (versionCode 218):** Central de Integrações reorganizada com cartões compactos, telas específicas para cada serviço e guias de configuração em português; a aba Redes coloca publicar e contas em primeiro plano, mantendo autorizações reais, testes opcionais e compartilhamento manual. Versão de código-fonte; ainda requer build Gradle e testes Android reais.

# Uai Cortes · versão 0.1.216 — divisão de vídeo revisada (versionCode 217)
- Revisão de Dividir vídeo: cópia MP4 cooperativamente cancelável, limpeza de itens não publicados, isolamento de callbacks antigos após nova tentativa, grupos de duração com memória de seleção e campos HH:MM:SS responsivos.
- Preserva 9,5/10/30/60s, presets em minutos e duração personalizada, limite 180 partes, Media3 e módulos existentes.
- Validação: contratos Python/Kotlin; build Android e teste real ainda pendentes.

# Uai Cortes · versão 0.1.215 — opções de divisão para redes sociais (versionCode 216)
- **Dividir vídeo**: cortes rápidos (9,5, 10, 30 e 60 s), em minutos (1, 1min05s, 2, 2min59s, 3, 4 e 8 min) e duração personalizada (HH:MM:SS entre 1 s e 23h59min59s).
- Cálculo de até 180 partes com última parte menor quando necessário, prévia precisa e avisos para trabalhos grandes. O app não garante requisitos de monetização; cada plataforma aplica condições próprias.
- Media3, salvamento de MP4/M4A, vídeo original e fluxo de resultado mantidos. Requer build Gradle e testes físicos no Android.

# Uai Cortes · versão 0.1.214 — Revisão de extração e arquivos incompletos (versionCode 215)
- **Extrair áudio**: continua com escolha SAF, progresso em tela cheia e resultado real. Agora há opção explícita de apagar somente o arquivo M4A parcial criado na tentativa cancelada/com erro, com diálogo de confirmação.
- A limpeza espera o fechamento da cópia; se o provedor de documentos rejeitar a exclusão, o usuário recebe mensagem para usar Arquivos. Não apaga automaticamente documentos nem a mídia original.
- Mantidos Dividir vídeo, edição, projetos, cinco variantes previstas e assinatura. Ainda requer compilação Android no CI e testes SAF com provedores reais.

# Uai Cortes · versão 0.1.213 — Extrair áudio em tela cheia (versionCode 214)
- **Extrair áudio**: após escolher o local de destino do M4A no seletor Android, acompanhar a operação em tela cheia. As fases informam quando o Media3 extrai e quando o aplicativo grava o arquivo final.
- Resultado mostra o arquivo M4A somente depois de uma gravação concluída, com opção de ouvi-lo, finalizar ou começar outra extração. Erro e cancelamento apresentam feedback específico; arquivo parcial no destino SAF pode precisar ser excluído manualmente.
- Mantidos os controles e as telas da divisão da 0.1.212, áudio AAC em M4A, vídeos originais, cinco APKs previstos e dados dos projetos.
- Compilação Android no GitHub Actions e teste físico permanecem pendentes.

# Uai Cortes · versão 0.1.212 — Dividir vídeo em tela inteira (versionCode 213)
- **Dividir vídeo**: o processamento agora ocupa uma tela própria, indicando exatamente qual parte está sendo cortada ou salva. Ao finalizar, a tela Resultado lista os MP4 e M4A realmente publicados e permite abrir cada arquivo, além de iniciar nova divisão.
- Cancelar pede confirmação, não apaga originais nem partes já gravadas. A barra avança apenas com partes completas; não é uma estimativa de FPS nem de bytes do Media3.
- Requer CI Gradle, Lint e validação visual/funcional em Android físico. Nenhum APK validado foi produzido nesta edição.

# Uai Cortes · versão 0.1.211 — Dividir vídeo responsivo (versionCode 212)
- **Dividir vídeo**: a seleção de 9,5/10 s se adapta a larguras menores e fonte ampliada; cartões de áudio e duração sinalizam estado para acessibilidade. Progresso informa a parte real, sem percentual fictício.
- Testar em dispositivos Android e tema escuro/AMOLED, inclusive fontes de 130% ou mais. O ambiente de edição não possui Android SDK e não conseguiu verificar compilação Gradle/CI no GitHub.

# Uai Cortes · versão 0.1.210 — interface Dividir vídeo renovada (versionCode 211)
- **Dividir vídeo**: interface por etapas, seleção de mídia destacada, cartões para 9,5 s/10 s e modos de áudio, prévia recolhível das partes e ação fixa no rodapé. Mesmos formatos, cópias e destinos anteriores.
- Essa evolução é visual. Requer validar compilação Gradle/Android Lint e testar em aparelho, principalmente telas pequenas, rolagem, acessibilidade e tema escuro.

# Uai Cortes · versão 0.1.209 — ferramentas do editor simplificadas (versionCode 210)

- A barra horizontal de edição não inclui mais Animar imagens nem Prompt Flow. Use **Animar fotos** e **Estúdio de Cenas** pela Home.
- Mantidos Corte, Transição, Legendas, Áudio, Marca d'água, Vozes IA e Descrição no editor, com Salvar no cabeçalho e inclusão de mídias na linha do tempo.
- A atualização mantém a proteção de mídias, rascunhos, schema 4 e exportação; conferir CI e testes no Android antes da distribuição.

# Uai Cortes · versão 0.1.208 — correção de regressão CI172 (versionCode 209)

- O workflow 172 parou antes do Gradle porque `tools/test_main_image_clip_169_contract.py` ainda procurava o antigo atalho **Imagem** na barra inferior do editor.
- O teste agora verifica a integração real da inserção de imagens pelo menu **Linha do tempo > Adicionar**, incluindo o encaminhamento a `onAddImage`. Não foram restaurados botões duplicados nem alterada a lógica de inserção/exportação.
- Mantidas as melhorias de 0.1.205–0.1.207: cópias de mídias, Salvar no cabeçalho, Animar imagens em posição visível e Sobre sem duplicar o histórico. `schemaVersion=4` permanece intacto.
- Testes de contrato e verificação do pacote-fonte podem passar localmente; Gradle Android, Android Lint e os cinco APKs Release ainda dependem de novo CI. Não há APK validado nesta entrega.

# Uai Cortes · versão 0.1.207 — proteção de mídias, editor e informações (versionCode 208)

- **Parte 1 · 0.1.205 (versionCode 206):** cópia automática de vídeos, áudios e PiPs, com MediaStore e confirmação antes do uso, reuso por projeto, limpeza de cópias parciais e proteção conservadora de projetos antigos ao reabrir. Imagens da faixa principal mantêm a cópia privada anterior.
- **Parte 2 · 0.1.206 (versionCode 207):** Salvar fixo no cabeçalho; Reparar mídias no menu; Animar imagens em destaque e retorno ao editor; Vídeo/Imagem/PiP inseridos pela linha do tempo.
- **Parte 3 · 0.1.207 (versionCode 208):** Sobre exibe um único destaque, desenvolvedor Adriedson Lemos, cópia da chave Pix e GitHub obtido do repositório embutido no build; Novidades no topo das Configurações e fluxo pós-atualização preservado.

**Armazenamento:** Android 10+ usa Filmes/Música/Imagens conforme o tipo, sob `UaiCortes/Projetos/<ID>`; Android 8/9 usa cópia privada. Os originais não são movidos ou apagados. Cópias de mídia não substituem backup externo; projetos antigos devem ser salvos após a proteção.

**Validação:** testes estruturais e scripts locais; `:subtitle-core:test`, `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleRelease` ainda precisam ser executados no CI. Testar em aparelho cópias MediaStore, exportação e interface; não há APK validado localmente.

# Uai Cortes · versão 0.1.204 — Subtitle Studio: animações suaves (versionCode 205)

**Subtitle Studio, segunda etapa:** Pop suave, Cinema suave, Fade delicado, Impacto limpo e Deslizar suave; curvas temporais determinísticas implementadas no módulo `:subtitle-core` e usadas na prévia Compose e na exportação Media3. Os oito estilos recomendados passam a usar novas curvas quando apropriado, mantendo todos os 25 modelos legados e animações anteriores. As animações usam tempo absoluto, sem rede e sem Lottie/libass; velocidade existente (0,5×–2×) continua válida.

**Compatibilidade:** esquema de projeto 4, textos originais, SRT, horários por palavra, personagens, vozes Gemini, mídia e cinco variantes APK preservados. Build Gradle, Lint e renderização MP4 real ainda precisam ser verificados no GitHub Actions e em aparelho.

# Uai Cortes · versão 0.1.203 — Correção CI169: fontes do Subtitle Studio (versionCode 204)

O workflow 169 compilou Kotlin, mas `:app:testDebugUnitTest` reprovou 1 de 636 testes: `SubtitleTypographyTest.fontsAreLocalAndUnknownFallsBack`. O teste ainda exigia três famílias, embora o Subtitle Studio 0.1.202 tenha acrescentado a fonte local Cursiva. Atualizado o contrato para quatro famílias e acrescentado round-trip da fonte Cursiva e fallback para fonte remota desconhecida. Nenhuma alteração de UI, renderer, fonte ou modelo de projeto foi necessária. Reexecutar o Gradle/JUnit/Lint completo no GitHub Actions.

# Uai Cortes · versão 0.1.202 — Subtitle Studio (versionCode 203)

O Subtitle Studio apresenta 8 presets modernos com os 25 antigos acessíveis, conversão visual para MAIÚSCULAS/minúsculas, uma família de fonte cursiva do Android e módulo puro `:subtitle-core` de transformação. O texto original, marcações por palavra, SRT, vozes Gemini e exportação de mídias são preservados. Gradle/Media3 ainda precisam ser validados no CI e no aparelho.

# Uai Cortes · versão 0.1.201 — Atualizações e Novidades

A tela Novidades exibe apenas os destaques da versão instalada na primeira abertura, com histórico opcional. A tela Sobre resume as três versões recentes. O atualizador integrado informa taxa de download, bytes recebidos e progresso, mantendo verificação SHA-256 e confirmação explícita antes de instalar.

## Novidades v0.1.200
- **Visual mais uniforme:** diálogos e cartões de Flow, Vozes IA, Legendas, Corte, Exportação e configurações Gemini passaram a reutilizar a escala `UaiShapes`.
- **Botões discretos:** ações principais usam cantos de 8 dp, sem mudar o processamento, áudio, vídeo ou projetos.
- **Compatibilidade preservada:** timeline, geração Gemini, galerias, backups e as cinco variantes de APK continuam presentes; Gradle/Lint e teste visual no aparelho ainda precisam de confirmação.

## Novidades v0.1.199
- Padrão visual unificado (`UaiShapes`) com cantos discretos e botões compactos; diálogo de saída preserva Salvar, Descartar e Continuar.
- PiP reduz o uso vertical da tela e permite expandir a prévia; Áudio permite Play/Pausa de cada faixa importada e música em separado.
- Extração avançada mantém fontes do projeto e vídeo externo, recolhida por padrão. O leitor de mídias e as ferramentas rápidas continuam disponíveis.
- Validação estrutural/local não substitui compilação no GitHub Actions nem teste Android do player e da exportação.

# Uai Cortes · versão 0.1.199 — Galeria de vídeos integrada

**Ferramentas rápidas:** os atalhos Dividir vídeo e Extrair áudio agora abrem a mesma galeria interna de Criar projeto e Inserir imagem, com miniaturas, busca e pastas. Toque em um vídeo para escolhê-lo. A opção **Procurar nos arquivos** permanece disponível na própria galeria como alternativa Android SAF.

**Preservação:** seleção única para ferramenta rápida, nenhuma alteração nos projetos existentes, mídias originais ou áudios anteriores. Divisão em 9,5/10 s e extração M4A continuam disponíveis. v0.1.198/versionCode 199, cinco APKs Release previstos. A compilação e o funcionamento em aparelho ainda exigem testes.

# Uai Cortes · versão 0.1.197 — Correção CI163 para Media3 Lint

**Correção:** A extração de áudio via Media3 agora utiliza `@androidx.annotation.OptIn(UnstableApi::class)` no próprio `AudioExtractionEngine`. O workflow 163 compilou Release, mas `:app:lintDebug` bloqueou a publicação; a alteração remove o requisito indevido sobre a tela de ferramentas rápidas, sem desabilitar as verificações.

**0.1.197/versionCode 198**. Funcionalidades da 0.1.196, cinco APKs Release, projetos schemaVersion 4 e assinatura preservados. Validação real Android/Lint depende de novo workflow.

# Uai Cortes · versão 0.1.196 — Divisor de vídeo e extração rápida de áudio

**Novo na Home:** atalhos Dividir vídeo e Extrair áudio. Vídeos de origem podem ser divididos em trechos de 9,5 s ou 10 s, gerando MP4s independentes sem precisar criar projeto. A opção M4A por parte é exclusiva de Android 10+; no modo Extrair áudio separado, o destino é escolhido via SAF.

**Funcionamento:** Media3 Transformer processa um trecho por vez; publicação na galeria por `IS_PENDING` com rollback; vídeo original não é alterado. Salva em Filmes/UaiCortes/Divididos; M4A por parte em Música/UaiCortes/Divididos. Limite de 180 partes por lote, com interrupção pelo usuário e preservação do que terminou. Cortes dependem de precisão de quadros/codec e devem ser testados em APK real.

**0.1.196/versionCode 197**, schema do projeto 4 mantido, 5 APKs Release previstos e demais recursos preservados.

# Uai Cortes · versão 0.1.195 — Correção de compilação CI161

A versão 0.1.195 corrige o build Debug/Release bloqueado no workflow **Gerar APK 161**: faltava importar `androidx.compose.foundation.layout.height` em `ProjectDraftUi.kt`. O diálogo de saída preserva as três ações horizontais **Salvar**, **Descartar** e **Continuar** com ícones, sem alterar sua lógica.

**v0.1.195/versionCode 196**: mantidos os cinco APKs previstos, assinatura, projetos schemaVersion 4, backups, Central de Vozes e recursos da 0.1.194. O novo teste de regressão é estático; build Gradle, JUnit, lint e testes Android precisam passar no GitHub Actions.

# Uai Cortes · versão 0.1.194 — Exportação e inserção de mídias

Após exportar um MP4, **Concluir** confirma o salvamento do projeto no catálogo e retorna à página inicial; caso a gravação do projeto falhe, o editor não é descartado e o vídeo exportado permanece disponível. **Voltar ao editor** continua independente. Projetos continuam em formato schemaVersion 4, com snapshots gerados somente após o commit confirmado.

**Salvar edição?** mostra as ações Salvar / Descartar / Continuar lado a lado com ícones, e Salvar como cópia para projetos já existentes. A ferramenta **Vídeo +** abre a galeria e, antes de modificar o rascunho, pergunta onde inserir: Início, Antes, Depois ou Final. A inserção de múltiplos clipes usa o mesmo deslocamento de legendas, áudio e PiPs da inserção de imagens, preservando mídias e links. A ferramenta **Inserir imagem** exibe as quatro posições na mesma linha e mantém o controle de duração. Barra inferior mais compacta, rolagem finita.

**v0.1.194/versionCode 195**: preservar cinco APKs Release, assinatura, Central de Vozes, histórico e backups. Build Android real, testes de layout em tela pequena e verificação física de sincronização após múltiplas inserções ainda são necessários.

# Uai Cortes · versão 0.1.193 — Vozes IA mais simples

Na tela **Vozes IA**, cada legenda agora aparece em uma linha com três controles: **ouvir/gerar**, **tom** e **voz**. A voz configurada no personagem continua sendo herdada; alterações por fala são exceções. Toque em uma fala para mostrá-la no vídeo; **mantenha pressionada** para encontrar Inserir/Trocar na timeline, Refazer, Sincronizar, Restaurar WAV original ou Remover faixa, conforme o estado da fala. Nada é gerado ao selecionar uma voz ou interpretação.

A barra mostra WAVs prontos em relação ao total e o número faltante, com **Vozes**, **Gerar** e **Inserir**. Personagens e detalhes da fila são recolhíveis para deixar a lista maior. Ao retornar a um projeto em que a geração já começou e ainda há falas faltando, um aviso oferece gerar pendentes ou revisar a fila salva. A API não é chamada automaticamente; filas com resultados incertos continuam exigindo revisão explícita. Salve o projeto no editor para confirmar a configuração escolhida.

**v0.1.193 (versionCode 194)**: formato de projeto 4, cinco APKs Release e cache de vozes existentes preservados. Compilação Gradle/Android e testes visuais no aparelho continuam obrigatórios.

# Uai Cortes · versão 0.1.192 — Legendas por Personagem

Na tela Legendas, abra **Mais > Estilos por personagem**, escolha Cliente, Agiota ou outro personagem e ajuste cor, fundo, fonte, tamanho, sombra, contorno, animação e posição. Confirme para aplicar a todas as falas daquele personagem que não possuem exceção individual. Nas falas isoladas, edite normalmente e use **Restaurar estilo do personagem** para voltar à herança. A associação de personagem já existente é reutilizada. Texto, horários, IDs e marcações de palavras não mudam na aplicação visual. As alterações fazem parte do rascunho; confirme **Salvar projeto**.

A edição não desloca a lista de legendas ao topo. É possível reativar o acompanhamento da reprodução pelo ícone de seguir. Vozes IA depende de pelo menos uma legenda válida; sem ela, é mostrado um caminho para Legendas, enquanto a Central de Vozes e a Narração permanecem disponíveis. No editor, a faixa Salvar/Cortar/Unir/... possui limite de início/fim sem repetição circular. Mudanças de duração de fotos reconstroem a playlist de prévia; pulos visuais exigem comparação física com o MP4 de origem e exportado.

Versão **0.1.192 / versionCode 193**, schemaVersion 4 preservado, cinco APKs no GitHub Actions e backups antigos compatíveis. Validações estáticas não são substituto do build Android e testes reais.

# Uai Cortes · versão 0.1.191 — Correção CI157

A versão 0.1.191 atualiza o teste JUnit da identidade de prévia de voz, que bloqueou o workflow **Gerar APK Uai Cortes 157**. O teste antigo exigia IDs diferentes para chaves Gemini distintas. A Central de Vozes 0.1.190 passou a reutilizar amostras WAV entre chaves, desde que modelo, voz, interpretação e texto fixo coincidam. Esse comportamento está preservado; o teste agora reflete o contrato real. Nenhuma geração é disparada pela troca de chave.

O projeto continua com catálogo persistente, cache durável de prévias, vozes e interpretações por personagem, cinco APKs Release, assinaturas e projetos existentes. Sem implementação de voz de narrador padrão/global ou específica por projeto nesta correção; isso fica para a próxima evolução.

**Validação:** código-fonte e testes locais não substituem o próximo workflow Android nem os testes reais em aparelho.

# Uai Cortes · versão 0.1.190 — Central de Vozes

Use **Vozes Google** na Home para acessar o catálogo sem abrir um projeto. O mesmo componente central aparece nos seletores de **Vozes IA** e **Narração**. A lista é persistida dentro do armazenamento interno do app; por padrão, não há consulta à API ao abrir. Toque em Atualizar para renovar a lista, ou habilite atualização automática. Falhas mantêm o último catálogo válido.

**Prévias:** "Olá! Este é o Uai Cortes. Aqui você corta vídeos, edita cenas e cria novas histórias. Fácil, não é? Agora... vamos ouvir esta voz!". Os WAVs de teste são gerados somente após confirmação de cota e ficam salvos por voz, modelo e estilo; ouvir novamente usa o arquivo local. Capacidade selecionável 50/100/250 MB, 100 MB padrão, limpeza manual sem alterar narrações inseridas. A biblioteca armazenada pode ficar antiga; um erro de geração pode exigir atualização manual. A desinstalação ou limpeza de dados apaga a biblioteca.

Em Vozes IA, escolha a voz principal nos cartões dos personagens e personalize o **Tom** de cada legenda. A barra compacta expõe Central/Tom/Gerar/Inserir. Ainda é necessário salvar o projeto pelo editor para guardar as preferências das falas. Arquivos e backups existentes, formato do projeto 4, compilação CI dos cinco APKs e assinatura foram preservados.

**Validação:** análise sintática, contratos estáticos, parser de identidade e integridade ZIP. Android completo e Gemini real pendentes.

# Uai Cortes · versão 0.1.189 — Voz e interpretação por personagem

Em **Vozes IA**, agora é possível configurar interpretação por personagem (por exemplo Assustada ou Sarcástica), além da voz Gemini. Cada fala herda os valores do seu personagem, exceto se houver configuração individual. O rascunho do projeto guarda as preferências; finalize com **Salvar projeto** no editor.

Na faixa dos personagens, os ícones permitem escolher voz, interpretação, ouvir um teste curto (uma chamada Gemini confirmada e que pode consumir cota) e solicitar somente falas pendentes. O painel **Revisar lote** mostra estados, áudios desatualizados, sincronização e pendências por personagem e permite gerar somente o filtro ativo. A seleção não gera WAV automaticamente nem insere faixas sem confirmação.

O aplicativo preserva a importação SRT e o estilo Karaokê IA, timeline, assinaturas, backups e JSON schemaVersion 4. v0.1.189/versionCode 190. Build Android e testes físicos pendentes após verificação de código e testes locais.

# Uai Cortes · versão 0.1.188 — Vozes por personagem e legendas unificadas

Em **Vozes IA**, use os personagens cadastrados em **Legendas** para atribuir uma voz Gemini a todas as suas falas. O projeto conserva a preferência por personagem e permite uma voz diferente numa legenda isolada. Filtro por personagem e pesquisa atuam apenas na lista; a timeline e a geração em lote continuam completas. A prévia agora inicia compacta e pode ser ampliada.

Ao gerar legendas automáticas, escolha **Clássica (SRT)** ou **Karaokê IA** e ajuste o tamanho. O modo clássico usa a aparência da importação SRT, sem perder horários por palavra disponíveis. Trocar configurações de voz não consome API; gerar WAV pode consumir cota. Configurações aplicadas no editor devem ser confirmadas com **Salvar projeto**.

**Validação:** testes de regressão e integridade local não substituem compilação Gradle, CI ou teste em aparelho. Versão de código-fonte 0.1.188, versionCode 189, mantendo schemaVersion 4 e cinco APKs Release previstos.

# Uai Cortes · versão 0.1.187 — Correção de compilação dos testes

A versão 0.1.187 corrige o erro de compilação do teste instrumentado `HomeAboutPreviewAndroidTest.kt` observado no workflow 154, removendo o import inválido de `assertDoesNotExist`. As funções de edição e atualização da 0.1.186 permanecem inalteradas.

O GitHub Actions continua configurado para testar, executar Lint e compilar cinco APKs Release. É necessário validar esta versão no CI antes de declarar os APKs compilados.

# Uai Cortes · versão 0.1.186 — Sobre e atualizações simplificados

- Atualizações disponíveis em **Sobre → Verificar atualizações**, sem editar o repositório. A origem é incorporada à compilação do GitHub Actions; builds locais sem origem apresentam aviso.
- Quando houver versão mais recente com APK universal e SHA-256, o próprio Uai Cortes baixa e verifica o arquivo, mediante confirmação. A instalação depende da aprovação do Android e do mesmo certificado de assinatura.
- Consulta de Releases usa um lote menor e limite maior de resposta, com erros de resposta excessiva diferenciados.
- Diálogo de saída do editor mais conciso, com Salvar, Descartar, Continuar e Salvar cópia para projetos já existentes. Projetos, rascunhos e backups foram preservados.

- A tela de Legendas automáticas prioriza o modelo disponível e os botões Offline/Gemini, sem parágrafos repetidos.
- Informações de privacidade, limites e revisão ficam na ajuda `?`, com avisos essenciais visíveis antes do envio.
- A confirmação Gemini exibe apenas envio de áudio, custo/cota e modelo; detalhes locais de uso podem ser expandidos.
- Atribuir personagem usa diálogo proporcional à quantidade de pessoas, com seleção manual e cores.
- Compatibilidade de projeto, Gemini, Vosk, salvamento e exportação preservada; compilação/testes Android pendentes.

# Uai Cortes · versão 0.1.184 — Linha do tempo de legendas compacta

A tela de legendas aproveita melhor o espaço do celular: título, player, seguir e fullscreen na mesma barra, Zoom 1×/2×/4× em menu do progresso, filtro de personagem em menu junto a Legendas, ações avançadas em Mais e geração automática reunida em Automáticas. A timeline sempre desenha todas as legendas, atenuando as que não correspondem ao filtro; a lista continua filtrada e tocar um bloco fora do filtro restaura Todas. O toque manual na lista pausa Seguir; o ícone permite retomar. O botão Apagar todas ainda confirma antes de excluir. Projetos, schemaVersion 4, transcrição e exportação preservados. Compilação e testes de toque no Android pendentes.

# Uai Cortes · versão 0.1.183 — Vozes sugeridas e revisão de personagens

A transcrição Gemini 3.5 Transcribe ganhou opção para pedir diarização e horários de palavras na mesma chamada após consentimento. Voz 1 e Voz 2 são apenas rótulos de voz, não nomes; no painel de revisão, o usuário pode criar personagens, vincular a personagens existentes ou deixar falas sem personagem. Texto, cortes, horários, cores e arquivos originais permanecem preservados; a aplicação só ocorre quando confirmada. Modelos Vosk locais não sugerem locutores. O API pode errar, cobrar ou não retornar rótulos. `schemaVersion=4`, cinco APKs Release no CI, testes Android em aparelho pendentes.

# Uai Cortes · versão 0.1.182 — Legendas por personagem

Nesta versão o editor de Legendas ganhou modo de prévia compacta/ampliada, zoom 1×–4×, indicação da legenda em reprodução distinta da selecionada e personagens manuais por projeto. É possível cadastrar, renomear, atribuir, filtrar e remover personagens sem apagar legendas; a aplicação da cor do personagem ao texto exportado exige confirmação. O reconhecimento automático de quem fala não foi implementado. Compatibilidade `schemaVersion=4`, cinco APKs previstos no CI; sem build Android físico local.

# Uai Cortes · versão 0.1.181 — Prévia de vídeo na tela de legendas

- A tela de Legendas reproduz o vídeo mesmo quando o projeto ainda tem zero legendas.
- Faixa de referência mostra os vídeos e imagens por tempo absoluto, com miniaturas sob demanda e cursor sincronizado.
- O botão Nova inicia a legenda no cursor escolhido, inclusive após avançar/pausar o vídeo.
- Preserva cortes, imagens estáticas, rascunhos, backups e `schemaVersion=4`; exportação MP4 não alterada.
- Testes estruturais e Kotlin isolado não substituem a compilação Android nem teste visual.

# Uai Cortes · versão 0.1.180 — Vosk: ZIP seguro, recuperação e diagnóstico

- Instalação voluntária de modelo de fala português: ZIP oficial, pasta externa adicional ou arquivos do modelo na raiz são aceitos após validação do formato correto.
- Extração verifica tamanho global, número de entradas, caminhos seguros, CRC e entradas duplicadas antes de instalar. Um modelo já válido não é substituído; instalação incompleta anterior tem proteção de recuperação.
- Erros exibem orientações claras para ZIP inválido, modelo errado, conexão, permissão e armazenamento; diagnóstico registra apenas códigos controlados, não nomes/caminhos de arquivos.
- Projeto, timeline, salvos/rascunhos e backups da 0.1.179 não são alterados; `schemaVersion=4` preservado. Testar instalação Vosk e reconhecimento real em Android após CI.

# Uai Cortes · versão 0.1.179 — Salvar, descartar e recuperar sessões

- A edição agora utiliza um rascunho separado da lista de projetos. Projeto novo só passa a existir na Home após Salvar. Ao sair com mudanças, o usuário escolhe Salvar, Salvar cópia (projeto existente), Descartar ou Continuar editando.
- Recuperação emergencial em arquivo privado `editor_draft_recovery.json`, com integridade SHA-256, validação da versão original e confirmação para restaurar. Backups históricos são feitos apenas de projetos confirmados.
- Configurações → Salvamento e segurança mostra pasta e tamanho dos backups locais e permite limpeza confirmada de cópias de projetos excluídos; arquivos originais e backups externos preservados.
- **Pendente:** testes Gradle Android, rotação/process death, comportamento de teclado e validação da prévia/exportação no aparelho. Ver `docs/EDITOR_SAVE_SESSION_0.1.179.md`.

# Uai Cortes · versão 0.1.178 — Reparar arquivos de projetos antigos

- O editor agora possui **Reparar mídias** (ícone de pasta). Ele lista mídias locais cujo arquivo ou permissão deixou de estar acessível e permite selecioná-las novamente pelo Storage Access Framework do Android.
- A recuperação valida tipo, duração suficiente para os cortes existentes e concessão persistente para vídeos/áudios; fotos são copiadas para armazenamento privado, usando a validação de importação existente.
- Nenhum corte, legenda, posição de PiP, trilha, ID ou arquivo original é modificado. Projetos não passam por migração (`schemaVersion=4`).
- **Pendente:** compilação Gradle Android, testes SAF e reprodução/exportação real após reconectar arquivos. A identidade do conteúdo não pode ser comprovada apenas por metadados; escolha o arquivo original.

# Uai Cortes · versão 0.1.177 — Teste de limite do PiP corrigido

- O workflow 147 falhou em `:app:testDebugUnitTest`: a implementação já aceitava tamanho de PiP até 150%, mas um teste antigo ainda rejeitava 91%.
- Atualizada a verificação JUnit para aceitar 91%, 100% e 150%; rejeitar valores fora de 5%–150%. A funcionalidade do PiP e o formato dos projetos foram preservados.
- **Pendente:** executar novamente testes completos, Android Lint e publicação de APKs no GitHub Actions. Não confundir validação de fonte com build Android aprovado.

# Uai Cortes · versão 0.1.176 — Novo enquadramento de imagens da faixa principal

- O editor oferece **Enquadrar imagem** no cartão da foto principal: dois modos, **Ajustar** (mostra tudo) e **Preencher** (amplia para cobrir o quadro), zoom de 100–300% e arrasto com um ou dois dedos. O arquivo original permanece intacto; Salvar aplica apenas à foto selecionada e Cancelar descarta o rascunho.
- `Clip.imageFrameMode`, `imageZoom`, `imagePanX` e `imagePanY` são campos JSON **opcionais**, com defaults `fit`, `1`, `0` e `0`; schemaVersion 4 não muda. PiP mantém `Overlay.fitMode` e campos próprios.
- Uma transformação de matriz `ImageFrameEffect` após a apresentação do Media3 utiliza a mesma política de geometria da prévia do editor. Fotos personalizadas prevalecem sobre o recorte global da exportação vertical. Conteúdo da foto não sofre alteração de pixels na origem.
- **Limitação:** a prévia adota quadro de referência do primeiro vídeo do projeto, ou 9:16 para projeto composto só por fotos. Se o destino de exportação tiver outra proporção, o enquadramento precisa ser revisado. Igualdade pixel a pixel exige build e MP4 real.
- **Pendente:** transições reais de duas faixas de vídeo sobrepostas (crossfade/virada 3D) requerem renderizador especializado; a própria documentação atual do Media3 Transformer não suporta crossfade nativo em Composition. As transições anteriores continuam funcionando.

# Uai Cortes · versão 0.1.175 — Edição mais direta, PiP sem margem e timeline compacta

- Campos numéricos de tempo não são apagados ao receber foco. A seleção inicial permite substituir os dígitos existentes; o cursor permanece ajustável e entradas inválidas não se tornam horários salvos.
- PiP pode chegar a **100% da largura** e até 150% do controle de escala, alinhando com as bordas. O modo padrão **Ajustar** preserva a proporção; **Preencher** usa zoom e recorte intencional. O novo `Overlay.fitMode` é opcional e assume `fit` nos projetos anteriores, sem mudar schemaVersion 4.
- Galeria de transições começa com seis efeitos existentes (Corte direto, Zoom, Recuo, Deslocamento, Flash e Fade preto), com cartões gráficos e simulação de quadros reais; os demais efeitos são acessíveis em Ver todos. NÃO é dissolvência real nem virada 3D.
- Timeline oculta faixas vazias e oferece botão **Adicionar** para todas as mídias. O conteúdo oculto nunca é removido do projeto e volta a aparecer quando inserido.
- **Ainda não implementados:** livre transformação das fotos de fundo/quadros principais e uma verdadeira dissolvência por sobreposição de quadros. Os ajustes de enquadramento desta versão se aplicam ao PiP de imagem/vídeo.
- Testes JUnit e Android do compositor Media3 continuam necessários antes de declarar fidelidade exata da prévia/exportação e touch no aparelho.

# Uai Cortes · versão 0.1.174 — Edição direta nas faixas da timeline

- **Ajuste fino:** zoom com botões 1×/2×/4×/8× e encaixe magnético opcional, que pode ser desativado para movimentação livre.
- **Editor:** seleção por toque sem trocar de tela; arraste o centro dos blocos para reposicionar, use alças de início/fim para aparar e zoom de 1× a 8× para precisão.
- **Clipes principais:** vídeos/fotos continuam sequenciais; arrastar reordena, aparar ajusta o trecho/duração e recalcula o projeto. Foto pode durar até 15 minutos ao redimensionar.
- **PiP, música e vozes:** PiPs/áudios não vinculados são movidos e aparados sem exceder a fonte ou o projeto; PiP de vídeo preserva offset da fonte em `sourceStartMs` (default legado 0). Voz vinculada permanece no clipe.
- **Legendas:** edição de horários e palavras existentes; confirmação ao soltar; histórico de 15 operações em memória e controle Desfazer/Refazer.
- **Proteções:** PiPs novos que colidirem sem autorização são recusados com orientação; elementos globais contidos em clipes reordenados acompanham sua posição, os atravessados exigem revisão.
- **Limites:** é necessário validar a prévia e o MP4 exportado num Android real; não afirmar compilação sem CI.

# Uai Cortes · versão 0.1.173 — Correção da importação de fotos

- Corrigido o fluxo do botão Imagem: o leitor de dimensões BitmapFactory em modo `inJustDecodeBounds` não rejeita mais `null` de `decodeStream` para uma imagem válida.
- Preservadas a galeria integrada, a cópia privada da imagem, as durações, o posicionamento e a linha do tempo.
- Testes instrumentados adicionados para JPG/PNG válidos, persistência e JPEG inválido. Execução em dispositivo Android ainda pendente.

# Uai Cortes · versão 0.1.172 — Editor sincronizado e mídia durável

- **Editor:** linha do tempo sincronizada com faixas de vídeos, imagens, PiP, áudio, vozes e legendas; cursor único e rótulos em ícones, sem mudar JSON do projeto.
- **Imagens:** usar a galeria integrada já existente para selecionar fotos; SAF alternativo e cópia privada por projeto para evitar a perda de acesso por URI temporária.
- **Diagnóstico:** erros de Gemini/Meta/TikTok/YouTube com código HTTP categorizado; nenhuma chave, token ou prompt é registrado.
- **Limites:** edição da mídia original continua não destrutiva. Reprodução/exportação precisam ser testadas no Android CI e físico.

# Uai Cortes · versão 0.1.171 (05/11/2026)

- **Editor de cortes simplificado:** prévia ocupando o espaço superior, uma régua visual com 12 miniaturas, cursor arrastável e ações grandes Dividir, Aparar, Remover. Ação Pronto retorna ao editor principal; ajuste preciso, mover, excluir clipe e refazer ficam no menu secundário.
- **Recorte visual:** alças roxas escolhem o que manter; alças amarelas escolhem o que remover. A remoção do intervalo pede confirmação, divide a mídia sem regravar o original, remonta a sequência, mantém a transição na borda final e ajusta horários globais de legendas e PiP.
- **Segurança e compatibilidade:** undo/redo de até 15 passos em memória, mínimo de 250 ms por trecho, proibição de apagar o único clipe inteiro e persistência em schemaVersion 4. Áudio vinculado a clipe mantém referências; MP4 final ainda depende de validação real.
- **Regressão:** testes do núcleo e controle visual adicionados. Sintaxe e estrutura verificáveis localmente; CI Gradle e teste no Android são obrigatórios antes de declarar funcionamento aprovado.

# Uai Cortes · versão 0.1.170 (05/11/2026)

- **Correção CI143:** `StaticImageClip.kt` declara `@androidx.annotation.OptIn(UnstableApi::class)` na função `ClipMediaItems.playback`, que chama `MediaItem.Builder.setImageDurationMs`. Corrige especificamente `UnsafeOptInUsageError` encontrado em `:app:lintDebug` do workflow **Gerar APK 143** (0.1.169).
- **Sem mascarar diagnósticos:** lint permanece ativo, sem baseline, supressões globais ou `abortOnError=false`.
- **Compatibilidade:** a função Imagem/Capa 0.1.169, editor, projeto JSON schemaVersion 4, Estúdio, Gemini, Redes Sociais e cinco APKs Release são preservados. Versão 0.1.170, versionCode 171.
- **Validação:** execução local do contrato para opt-in, testes estruturais e sincronização; compilação Gradle/Lint e teste do MP4 no aparelho ainda precisam ser executados na nova versão.

# Uai Cortes · versão 0.1.169 (05/11/2026)

- **Fotos como clipes principais:** no editor, a opção **Imagem** abre o seletor do Android (SAF), valida JPG/PNG/WebP/HEIC/HEIF/BMP, limita arquivo a 24 MiB e imagem a 24 megapixels, permite definir de 1 a 30 segundos e inserir como capa no início, antes/depois do trecho selecionado ou no fim, sem alterar arquivo original.
- **Prévia/edição:** foto na mesma faixa Vídeo dos demais clipes, com rótulo próprio e miniatura; tocar nela abre opções para alterar duração, duplicar, mover ou excluir. Prévia Media3 usa `MediaItem.Builder.setImageDurationMs`; captura de quadro de origem fica desabilitada em foto. Tela Duração usa amostragem de bitmap em IO.
- **Exportação real via Media3 Transformer:** imagem via `EditedMediaItem.Builder.setFrameRate(30)` com duração configurada, mantidos efeitos de vídeo e áudio de fundo. Extrator e reconhecimento de fala ignoram trechos de foto sem áudio, mas preservam a duração no deslocamento das legendas. **Falta testar MP4 e sincronismo em Android real; testes estruturais não substituem build**.
- **Sincronização:** ao inserir/resimensionar uma foto, os elementos com tempo absoluto posteriores (legendas com tempo por palavra, PiPs e áudio extra global) avançam junto com os clipes. Trilhas de áudio vinculadas a ID de clipe seguem o deslocamento automaticamente. Música global mantém início no zero. Elementos cujo intervalo cruza o ponto de inserção podem exigir revisão manual.
- **Compatibilidade:** `Clip.isImage` e `imageMimeType` opcionais no JSON (defaults para vídeo), schemaVersion 4 mantido. Preservar projetos legados, recursos do Estúdio, Gemini, Redes Sociais, cinco APKs Release e versionCode 170. Fotos dependem da permissão de leitura SAF ainda válida; arquivo movido/excluído fora do app precisará ser relocalizado.

# Uai Cortes · versão 0.1.168 (05/11/2026)

- **Meta oficial, sem App Secret:** cliente Facebook Login SDK 19.0.0, autorização pelo Android sob toque, consulta `/me/accounts` e vinculação visual da Página cadastrada. Os IDs são públicos e persistidos; tokens de Página ficam apenas em memória e a sessão do usuário é gerida pelo SDK. A ação "Desconectar Meta" revoga a sessão local do SDK, sem excluir as fichas do Uai Cortes.
- **Publicação com sessão Meta:** autorização separada de `pages_manage_posts` para publicar; se a permissão existir e a Página estiver autorizada na sessão, dispensa colagem do token temporário no diálogo, ainda exigindo confirmação e estado UPLOAD_REVIEW antes do primeiro POST. Modo de token manual preservado como alternativa.
- **Configuração da Meta:** `META_APP_ID` e `META_CLIENT_TOKEN` opcionais no workflow, validados como IDs públicos no Gradle. Sem valores, o botão de login fica desabilitado e a publicação assistida continua disponível. App Secret é proibido no APK. Registre os hashes reais da assinatura Android e solicite revisão de `pages_show_list`, `pages_read_engagement`, `pages_manage_posts` nos produtos Meta. Não declarar produção habilitada sem aprovação.
- **TikTok:** central explica que Login Kit exige domínio verificado com HTTPS, certificado de assinatura e um backend próprio para troca de código por tokens, sem armazenar `client_secret` no cliente. A chave pública `TIKTOK_CLIENT_KEY` é opcional e não realiza login sozinha. Modo avançado de token temporário e status continuam disponíveis; OAuth TikTok integrado ainda pendente.
- **Compatibilidade:** não muda schema social 1 nem VideoProject schema 4, Editor, Gemini, Wizard ou o OAuth YouTube. Mantém 5 APKs Release e versionCode 169. A presença do SDK adiciona dependência a validar no CI; build, login, publish e visual Android real não foram executados localmente.

# Uai Cortes · versão 0.1.167 (05/11/2026)

- **Facebook Página:** vínculo de ID público confirmado pela Graph API v26.0 usando um token OAuth de Página inserido apenas para aquela sessão; START → upload binário → FINISH de Reel, cada envio exige consentimento e a conta deve corresponder à Página vinculada. Recebimento da API não garante processamento/público.
- **TikTok:** consulta oficial `creator_info` antes do envio para mostrar identidade, opções de privacidade e restrições de interação; seleção consciente, marcação opcional de conteúdo criado com IA, upload `FILE_UPLOAD` dividido em blocos, e consulta manual do status via `publish_id`.
- **Segurança:** o token OAuth é inserido no modo avançado, não persistido nem exportado; não há App Secret no APK, nem troca automática de tokens ou reenvio silencioso. A conexão não é login OAuth integrado, e o usuário deve obter token autorizado externamente. `UPLOAD_REVIEW` é gravado antes de qualquer POST; falha incerta exige inspeção da plataforma. `REMOTE_SUBMITTED` NÃO é confirmação de publicação pública.
- **Contas e compatibilidade:** perfil pessoal Facebook e Kwai permanecem manuais. `remotePageId` opcional não altera o schema social 1 nem `VideoProject` schemaVersion 4. Migração de cadastros 0.1.165/166, Gemini, Editor, Wizard e YouTube preservados. Metadados, fluxo de cinco APKs Release e versionCode 168 sincronizados.
- **Configuração externa:** Meta exige permissões aprovadas `pages_manage_posts`, `pages_read_engagement`/`pages_show_list`; TikTok exige `video.publish` e auditoria para visibilidade pública. No modo atual, tokens são fornecidos por ação, NÃO existe autenticação persistente nem OAuth integrado Meta/TikTok. Não publicar sem permissões reais.
- **Limites e testes:** nenhum token real, publicação real ou build Android foi validado nesta sessão; o cliente inclui testes de contrato, parser Kotlin, testes instrumentados pendentes e checagens de integridade do pacote. Upload depende de disponibilidade do serviço, formatos aceitos e rede estável; nunca presumir vídeo publicado.

# Uai Cortes · versão 0.1.166 (05/11/2026)

- **YouTube OAuth:** integração opcional com o AuthorizationClient do Google Play services (escopos `youtube.upload` e `youtube.readonly`). Vincular ID público do canal depois de consulta real e confirmação; nenhum token salvo/exportado.
- **Enviar ao YouTube:** botão por publicação preparada, confirmação individual, privacidade Privado/Não listado/Público (privado por padrão), inicialização de sessão recuperável e envio em blocos de 1 MiB. Sem envio automático de múltiplos destinos nem repetição cega após falha.
- **Status e métricas:** registrar `UPLOAD_REVIEW` antes do POST e `CONFIRMED_BY_API` somente quando API confirmar ID de vídeo. Atualizar métricas sob demanda com origem `youtube_api`, sem misturar valores manuais.
- **Compatibilidade:** campos novos opcionais em contas/publicações/métricas; JSON social ainda schema 1, projetos de vídeo `schemaVersion=4` e biblioteca do Estúdio preservados. Meta, TikTok e Kwai mantêm compartilhamento assistido; sem prometer upload automático.
- **Configuração externa necessária:** habilitar a YouTube Data API v3 e cadastrar OAuth Android com package `br.com.uaicortes` + SHA-1 da assinatura realmente usada; Google Play services no aparelho. Requer consentimento da conta e cotas do projeto Google Cloud.
- **Limites:** uploads acontecem com a tela de publicação ativa. Após interromper a sessão ou erro inconclusivo, o app exige verificação manual no YouTube Studio; nesta versão não persiste URI sensível de retomada. Build Android, OAuth real, compatibilidade de API e upload com conta autorizada ainda não foram executados localmente.

# Uai Cortes · versão 0.1.165 (05/11/2026)

- **Redes Sociais:** nova aba no rodapé da Home, de Projetos e de Ajustes com cadastro local independente de TikTok, YouTube, Kwai, Facebook Página e Facebook Perfil. O cadastro não significa que a conta está autenticada.
- **Após exportação:** opção Publicar nas redes para o exportador tradicional e Animar Imagens. Escolha do MP4 pelo SAF, múltiplos destinos, descrição/hashtags comuns, compartilhamento manual individual e histórico persistente. Abrir o seletor não confirma postagem.
- **Histórico e métricas:** estados preparada/compartilhamento aberto/publicação confirmada manualmente; ligação por vídeo e destino; visualizações, curtidas, comentários e compartilhamentos apenas quando informados. Nunca confundir números manuais com dados oficiais.
- **Central de APIs e Contas:** acesso às chaves Gemini existentes, links oficiais de desenvolvedor e exportação/importação SAF de cadastros sem credenciais, URLs ou tokens. Testes de API e uploads diretos dependem de OAuth/permissões oficiais, ainda não implementados.
- **Segurança:** catálogo JSON privado com AtomicFile, preservação de arquivo corrompido, verificação de URLs HTTPS, sem tocar em VideoProject schemaVersion 4 e sem criar serviços/contas falsas. Nenhum envio de vídeo ou chamada de rede automática.
- **Validação:** verificações estruturais/PSI e instrumentados incluídos. Build Android/execução de OAuth e publicação real permanecem pendentes de ambiente e credenciais autorizadas. Detalhes em `docs/VALIDATION_0.1.165.md`.

# Uai Cortes · versão 0.1.164 (05/11/2026)

- **Wizard em cinco etapas:** novo projeto abre primeiro o estilo visual; depois personagens, cenários, montagem dos clipes e exportação para Flow. Cada tela foca uma tarefa, com navegação e autosave.
- **Sugestões imediatas sem API:** alienígena, pessoa, animal, fruta, robô, objeto animado e cenários. Fichas com nome e aparência em destaque; roupa, voz e objetos opcionais.
- **Biblioteca entre histórias:** personagens, cenários e estilos podem ser salvos e reutilizados; imagem opcional da galeria com cópia privada. Exportação ZIP inclui referências importadas.
- **Gemini opcional:** enriquecer individualmente personagem ou cenário após confirmação, com revisão antes de aplicar; sem chamadas pagas automáticas.
- **Cena = clipe:** editor oferece personagens cadastrados e seleção de quem fala; imagem inicial opcional, prompt preparável sem IA. Compatibilidade dos rascunhos v1 e projetos de vídeo schemaVersion=4.
- **Validação:** testes locais estáticos e de integridade não substituem build Android/uso real das APIs; ver `docs/VALIDATION_0.1.164.md`.

# Uai Cortes · versão 0.1.163 (05/11/2026)

- **Cada cena é um clipe:** o fluxo agora permite criar Cena 1, Cena 2 etc. manualmente, sem história obrigatória; cada uma tem ação, fala, câmera, sons, imagem inicial e prompt próprios.
- **Gemini opcional:** pode sugerir história/cenas, aprimorar um único clipe, gerar imagem vertical ou deixar o usuário preparar localmente o prompt em inglês, sem cobrança.
- **Chave compartilhada:** seleção da credencial em Configurações aplica-se às novas chamadas de IA; Estúdio de texto, imagem e listagem de imagens agora respeitam alternância 429 por projetos distintos quando habilitada.
- **Limites:** não repetir 503, timeout, erros de autorização ou respostas incertas; modelo de outro projeto não é automaticamente trocado. Testes Android e chamadas reais seguem pendentes.

# Uai Cortes · versão 0.1.162 (05/11/2026)
- **Estúdio de Cenas mais simples:** apenas a ideia na entrada, com opções de duração/estilo/gênero recolhidas, resumo legível de título/sinopse/roteiro e campos detalhados sob demanda. "Surpreenda-me" fornece sugestão local imediata e explica que não usa Gemini.
- **Cenas inteligentes:** Gemini solicita falas por personagem e duração estimada por cena; cartões mostram cena, personagens, falas, câmera, sons e ações sem longos formulários. Edição estruturada de falas, menu de ferramentas e um botão principal por vez.
- **Progresso visível:** operação ativa aparece junto a indicador indeterminado e controle de cancelamento Gemini, com aviso sobre eventual consumo incerto. Exportação/importação mostram tarefa em curso.
- **Compatibilidade:** preserva rascunhos anteriores, formatos de vídeo `schemaVersion=4`, editor manual, controle de Gemini, credenciais e SAF. Testes de compilação Android e chamadas reais com chave ainda necessários. Consulte `docs/VALIDATION_0.1.162.md`.

# Uai Cortes · versão 0.1.161 (05/11/2026)
- **Estúdio de Cenas:** etapas História → Cenas → Exportar para Flow, com geração de roteiros/cenas por Gemini somente após consentimento e revisão antes de aplicar. Duração, estilo, gênero, roteiro existente e rascunhos recuperáveis.
- **Cenas e continuidade:** fichas persistentes de personagens e cenários, estado inicial/final, reordenação/duplicação/exclusão, edição por seção, geração de roteiro individual, importação de quadro final, visualização de imagens e associação manual de vídeos.
- **Imagem Gemini:** seleção após consulta `models.list` com a chave ativa, solicitação `generateContent` só para IMAGEM, proporção 9:16, confirmação explícita com custo verificável parcial ou aviso de custo desconhecido; importação, salvamento, regeneração e compartilhamento Android/Flow.
- **Exportação e compatibilidade:** ZIP SAF com imagens por cena, prompts UTF-8, roteiro, fichas e ordem; editor manual de Flow e refinamento em lote existentes permanecem em Avançado. `schemaVersion=4` e projetos legados preservados. Chamadas pagas e integração com Flow não testadas sem chave/autorização. Consultar `docs/VALIDATION_0.1.161.md`.

# Uai Cortes · versão 0.1.160 (05/11/2026)
- **Consumo mais compacto:** resumo de chamadas/tokens/429, custos e resultados por função; gráficos e detalhes técnicos recolhidos. Ajuda contextual `?` e uma linha por modelo utilizado, sem mostrar por padrão três modelos inativos.
- **Configuração unificada:** limites RPM/TPM/RPD e preços USD/1M numa janela por projeto/modelo. Estado do plano pago/gratuito escolhido pelo usuário; `0 USD` exige confirmação explícita de gratuidade daquela operação.
- **Referências oficiais datadas:** botão de preencher sugestão para Gemini 3.8 TTS, 3.5 Transcribe e TTS 2.5 compatíveis; dados consultados em 05/11/2026 na documentação de preços, com vigência e controle de frescor; nunca atualizar valores ou assumir plano via API. Modelos não cadastrados exigem valores manuais.
- **Gráficos corrigidos:** a barra de sucesso representa sucessos/tentativas, não volume relativo. Não altera históricos, chaves AES-GCM, cotas, recuperação, lotes, vídeo nem schemaVersion=4. Validação: `docs/VALIDATION_0.1.160.md`.

# Uai Cortes · versão 0.1.159 (05/11/2026)
- **Prompts Flow + Gemini Batch:** integração opcional dentro da tela Flow (projetos de vídeo e conjuntos locais). Seleciona 2–8 cenas não vazias por envio, modelo de TEXTO compatível, chave/projeto existente, confirmação de possível cobrança e consentimento explícito por envio. Contexto e cena textual são enviados; estilo-base, vídeo, áudio, imagem e credenciais NÃO entram nos prompts.
- **Acompanhamento e revisão:** job `batches/...` fica associado ao identificador local do Flow, números das cenas, modelo, projeto, perfil e SHA-256 do estado original; não persiste texto ou resultados do Gemini. Consulta estado manualmente e oferece revisão individual; só aplica se o resultado é válido e a cena/contexto não mudaram. Preserva original quando houve alteração.
- **Evita duplicidade:** checkpoint REVIEW é salvo antes do POST; jamais reenvia automaticamente após erro ou desconexão. Permite listar jobs do perfil e vincular manualmente um identificador remoto a envio incerto com confirmação. Bloqueia o mesmo conjunto de cenas enquanto há job potencialmente ativo.
- **Parsing REST Batch:** corrige leitura do envelope oficial `output.inlinedResponses.inlinedResponses` e reconhece falhas por item; mantém o uso anterior do Batch genérico. Sem geração TTS, upload de mídia, serviço em segundo plano, estimativa de saldo oficial ou aplicação automática.
- **Validação:** ver `docs/VALIDATION_0.1.159.md`. Preservar schemaVersion=4, 5 APKs Release, histórico e dados anteriores.

# Uai Cortes · versão 0.1.158 (05/11/2026)
- **Gemini Batch TEXTO opcional:** ferramenta expansível na página de Google Gemini. Envia de 2 a 8 pedidos curtos independentes, com escolha explícita de modelo de texto e aceite de possível cobrança do plano pago; nenhum job é criado ao abrir a tela. Só usa uma chave/projeto selecionado, sem failover, retry ou processamento local em segundo plano.
- **Gerenciamento seguro de jobs:** registra ANTES do POST um marcador de envio incerto para evitar duplicações em perda de conexão; após sucesso, guarda `batches/...`, projeto, perfil, modelo, estado e quantidade. Consultas são manuais; permite recuperar um job pelo identificador quando foi criado externamente, consultar estado e selecionar/copiar resultados inline. O registro local pode ser ocultado sem cancelar job remoto.
- **Privacidade:** prompts e respostas não são persistidos, apenas metadados privados de jobs (`gemini_batch_local_v1` fora dos backups). Falta de ID remoto exige revisão; nunca reenvia automaticamente. Texto enviado ao Google somente após confirmação; não altera WAVs, timeline, legendas nem MP4.
- **Limites reais:** API Batch tem cotas separadas, é assíncrona, pode levar até 24 h e a redução anunciada de preço depende do modelo/plano; o Uai Cortes não afirma saldo ou custo oficial. Não é Batch TTS e não sintetiza vozes desta tela.
- **Validação:** ver `docs/VALIDATION_0.1.158.md` para testes executados e pendências. Manter schemaVersion 4, cinco APKs Release e ferramentas anteriores.

# Uai Cortes · versão 0.1.157 (05/11/2026)
- **Consumo Gemini:** painel com gráfico compacto por função e tendência diária (até sete dias visíveis), sucesso/erro e latência média REAL de chamadas registradas por TTS, legendas e descrição/Flow. Nenhum tempo é simulado para operações anteriores.
- **Tarifas manuais opcionais:** campos separados de USD/1 milhão tokens de entrada e saída por projeto/modelo; só somar operações com ambos os valores de uso informados pela API. Não é saldo oficial, fatura nem cálculo garantido de custo para modalidade, cache ou nível diferente.
- **Estimativa antes do lote:** após cinco gerações comparáveis com tokens completos e tarifa informada, exibe custo local aproximado e identifica amostras insuficientes. Nunca altera modelo, chave ou cota da API.
- **Privacidade:** sem guardar prompts, WAVs ou chaves no histórico; preços ficam em preferências privadas, fora do backup do projeto; schemaVersion 4, fluxo de exportação e cinco APKs preservados.
- **Validação:** testes Kotlin/JVM + contratos Python e análise sintática. Compilação Android, preços reais e aparência no dispositivo ainda precisam de CI/teste físico.

# Uai Cortes · versão 0.1.156 (05/11/2026)
**Gemini 0.1.156:** recuperação local e manual de legendas e descrição geradas, com checkpoint atômico temporário privado e validação por fingerprint; chamadas interrompidas não são repetidas automaticamente. Diagnóstico de erros categorizado e consumo agrupado por operação no painel Gemini. Sem alterar chaves, mídia de origem, projetos JSON (schema 4) ou exportação. Build Android e testes reais pendentes.

# Uai Cortes · versão 0.1.155 (05/11/2026)
- **Vozes IA:** fila local persistente para gerar lotes com confirmação de cota, retomada manual e sem serviço em segundo plano. O registro contém IDs, fingerprints e opções de voz, não a chave nem o texto das legendas.
- Cada pedido registra **em andamento** antes da chamada Gemini e **concluído** somente depois do WAV salvo; ao reabrir, pedidos em andamento sem WAV verificado exigem revisão explícita para não repetir cobranças involuntariamente.
- Ações compactas Retomar, Revisar e Descartar fila. Alterações de legenda invalidam as pendências correspondentes. WAVs e timeline permanecem intactos após descartar o planejamento.
- Rascunhos e fila com limite de 500 falas e até 1 MiB por manifesto para suportar vídeos longos. Sem alteração do schemaVersion=4, credenciais, exportação ou limites da API.
- Regressões Kotlin/JVM e Python incluídas. Android/Compose, reinício real de processo e geração Gemini precisam de validação no GitHub Actions/aparelho.

# Uai Cortes · versão 0.1.154 (05/11/2026)
- **Consumo Gemini:** agora e hoje (horário do Pacífico) aparecem separadamente; RPM, TPM de entrada e RPD mantêm significados próprios. Mostrar tempo aproximado até abrir a próxima janela RPM, quando há dados locais suficientes.
- **Vozes IA:** planeja quantas chamadas novas são necessárias, contando WAVs existentes e válidos apenas quando a configuração da fala coincide. Arquivos concluídos permanecem disponíveis para retomar após interrupções.
- **Ritmo RPM interativo:** respeita o limite manual por projeto e modelo ao gerar lotes, aguardando a janela seguinte com opção de cancelar; limite diário local informado é verificado antes da chamada. Sem limites configurados, mantém operação anterior.
- Não consulta saldo oficial; não soma chaves do mesmo projeto nem repete chamadas ambíguas; não altera exportação, credenciais, legendas, mídias ou schemaVersion 4.
- Testes Kotlin/JVM e contratos locais incluídos; compilação Android/fluxo Gemini real dependem de CI e testes físicos.

# Uai Cortes · versão 0.1.153 (05/11/2026)
**Vozes IA:** agora permite favoritagem local das vozes Gemini, filtro de favoritas, nome amigável no seletor geral e porcentagem real por WAV finalizado em lote. Durante cada chamada individual, o progresso permanece indeterminado porque a API não fornece avanço parcial; nenhuma cota é estimada por quantidade de chaves. As chaves, legendas e a timeline não foram alteradas.
**CI128 — Publicação corrigida:** o workflow 128 passou em build/testes/Lint e verificou cinco APKs, mas a publicação foi rejeitada pela API GitHub porque as notas completas excedem 125.000 caracteres. O workflow gera notas curtas da versão atual sem apagar CHANGELOG/RELEASE_NOTES, testa o limite e atualiza notas de Releases existentes. Não altera o funcionamento Gemini ou a exportação de vídeo. Publicação v0.1.152 em CI ainda pendente.

# Uai Cortes · versão 0.1.151 (05/11/2026)
**Gemini 0.1.151:** novo painel de previsões locais por projeto/modelo/função, com médias de tokens de operações bem-sucedidas (mínimo 5 amostras) e limites RPM/TPM de entrada/RPD informados pelo usuário a partir do AI Studio. Não é uma consulta de saldo oficial; eventos antigos sem projeto não são atribuídos retroativamente. Dicas antes de gerar voz, legenda, descrição e Flow. Failover autorizado após HTTP 429 com pausa por projeto e modelo, sem mudar segurança das chaves. Compilação Android e testes de API reais pendentes.

# Uai Cortes · versão 0.1.150 (05/11/2026)
**CI126:** teste JUnit da exportação corrigido para reconhecer 480p como resolução válida desde a v0.1.147. O teste ainda rejeita resoluções não suportadas; nova regressão cruza a lista aceita pelo coordenador, pela UI e pelo JUnit. Não altera o comportamento da exportação. Código-fonte v0.1.150 / versionCode 151; confirmação Gradle completa depende do CI.

# Uai Cortes · versão 0.1.149 (05/11/2026)
**CI125:** correção de compilação Kotlin do painel Gemini: `Icons.Default.LockOutline` não é resolvido pela configuração atual de Material Icons; troca por `Icons.Default.Lock`. Nenhuma mudança no armazenamento de chaves, no layout funcional ou na geração de áudio. v0.1.149 / versionCode 150. APK depende do CI Android.

# Uai Cortes · versão 0.1.148 (05/11/2026)
**Gemini v0.1.148:** redesenho real da configuração de API: cabeçalho com status e campo protegido, atalhos de seis ações na barra compacta, cartões de chaves com selo de ativa, cadastro/edição em janela personalizada em vez de formulário comprido, alternância 429 com ajuda expansível e indicadores de consumo local com histórico estruturado. A troca de chave, a confirmação de cópia/exportação, os limites e a geração Gemini continuam inalterados. Validar composição/rolagem no APK Android.

# Uai Cortes · versão 0.1.147 (04/11/2026)
**Exportação v0.1.147:** novas resoluções econômicas 360p e 480p além de 720p e 1080p, opções maiores; progresso do Media3 explicado sem prometer velocidade linear. Tela final de vídeo salvo compacta, com ações Gerar descrição, Gerar com IA e Compartilhar; descrição, hashtags, consulta de modelos e consentimento Gemini somente em janela própria, sem envio do MP4 à IA. Build Android pendente.

# Uai Cortes · versão 0.1.146 (04/11/2026)
**Google Gemini v0.1.146:** página de API compacta, seis ações numa barra, botões de 8–9 dp de raio, chave com status protegido, cartões expansíveis para segurança e múltiplas chaves e indicadores locais de uso. Sem alterar segurança, tráfego TTS ou projetos.

# Uai Cortes · versão 0.1.145 (04/11/2026)
**Vozes IA v0.1.145:** seletor de voz Gemini com janela ampla e responsiva, filtros de gênero/timbre sem corte lateral, pesquisa sempre visível e lista mais densa com destaque da seleção atual.


**Vozes IA v0.1.144:** sincronização assistida com análise de conflito, ajuste local moderado de WAV que preserva o original, regeneração por ritmo confirmada, catálogo filtrável por características reais e botões compactos.

# Uai Cortes · versão 0.1.143 (04/11/2026)

## Vozes IA: barra única de seis ações e seletores organizados

- Ações **Buscar**, **Lite/Flash**, **Voz**, **Humor**, **Gerar N** e **Inserir N** reunidas em uma linha; botões retangulares compactos (40 dp, raio 8 dp), com descrição acessível completa e sem perder os contadores.
- Modelo Lite/Flash agora usa o mesmo estilo de botão; controles dos cartões individuais também ganharam cantos discretos, preservando textos, informações técnicas, prévia e timeline.
- Diálogos de vozes e estilos ordenados alfabeticamente em português; lista interna rolável limitada a 310/320 dp, seleção atual marcada e título compacto. Todas as opções originais permanecem disponíveis.
- Rótulos curtos aparecem somente na interface. Nenhum texto, estilo Gemini, áudio, chave ou duração do projeto é alterado por essa revisão.
- Testes locais e validação estrutural não substituem compilação Gradle nem teste no celular.

# Uai Cortes · versão 0.1.142 (04/11/2026)

## Vozes IA: interface compacta e escolhas gerais

- **Buscar vozes**, seletor de modelo **Lite/Flash** e **Voz geral** reorganizados na mesma barra da ferramenta Vozes IA. Escolher uma voz geral pede confirmação antes de substituir as escolhas individuais e não aciona a API.
- Botão **Humor** junto a **Gerar todas**, com os estilos existentes do Gemini (Humorística, Sarcástica, Neutra etc.) aplicáveis em lote mediante confirmação. Cada legenda continua permitindo escolha individual de voz e estilo.
- Removido **Voz para todas** da linha de geração. Cartões menores, texto recolhido em uma linha, seletores compactos e ações **Gerar/Refazer**, **Ouvir** e **Inserir/Substituir** preservadas.
- Prévia da tela Vozes IA agora reutiliza `InlineVideoPreview`, a geometria adaptativa da tela Legendas, sem editar posição, texto, tamanho ou tempos salvos. Horários em décimos (`00:00.5`) e sinalização em décimos para WAV que ultrapassa a janela original da legenda.
- Novo `VoiceIaDisplayPolicy` com testes JVM para ultrapassagens e arredondamento visual; nova regressão estática do layout e das decisões de confirmação. Áudio tradicional, timeline Vozes, Gemini TTS, schemaVersion 4, Media3 e cinco saídas Release preservados.
- A compilação Android, o fluxo Gemini real e a reprodução/exportação precisam ser confirmados no GitHub Actions e em aparelho. Não há APK gerado nesta entrega de fonte.

# Uai Cortes · versão 0.1.141 (04/11/2026)

## Correção CI118 — teste unitário de Vozes IA

- O GitHub Actions 118 compilou Kotlin Debug/Release e executou 456 testes unitários, mas a etapa `:app:testDebugUnitTest` falhou em um teste: `VoiceIaPolicyTest.insertAndReplaceKeepOnlyOneVoiceWithoutTouchingMusic`, na chamada de `VoiceIaPolicy.merge` (linha 24).
- A geração da URI do WAV usava `android.net.Uri.fromFile()` dentro do código chamado pelo teste JVM. A API do Android não é implementada no mockable `android.jar` usado por testes locais; o método foi substituído por `java.net.URI` padrão, preservando URIs `file:///...` e percent-encoding de espaços e caracteres acentuados.
- Regressão JUnit adicionada para URI de arquivo local sem Android, incluindo nomes com espaços e acentos; testes existentes continuam verificando inserção/substituição de voz e preservação de faixas tradicionais.
- Sem alterações no contrato da Gemini, chave, legendas, motor de exportação ou esquema de projetos. Código-fonte v0.1.141 com `versionCode=142` e workflow sincronizado.
- A execução integral de Gradle, lint, publicação dos cinco APKs e testes reais de reprodução/exportação continuam pendentes do GitHub Actions.

# Uai Cortes · versão 0.1.140 (04/11/2026)

## Correção do workflow 117 — inclusão do GitHub Actions no ZIP

- A entrega do código-fonte v0.1.139 foi empacotada sem o diretório oculto `.github`, deixando o workflow antigo (`VERSION=0.1.138`) no repositório após a atualização. O teste `check_version_sync.py` impediu a compilação ainda na validação inicial.
- O pacote desta versão inclui explicitamente `.github/workflows/android-apk.yml`, agora com `VERSION=0.1.140` e `VERSION_CODE=141`, em sincronia com Gradle, `VERSION`, metadados, Sobre/Novidades e empacotador de APKs.
- O ZIP é criado por `python3 tools/package_source.py --output <arquivo.zip>`, que verifica arquivos indispensáveis, executa `validate_source.py` sobre conteúdo extraído e rejeita ZIPs incompletos. Novo contrato protege a presença do workflow nesse empacotamento.
- Sem alterações no código funcional de Vozes IA, áudio, legendas ou exportação. A compilação Android, os cinco APKs Release e testes em aparelho ainda dependem do próximo GitHub Actions.

# Uai Cortes · versão 0.1.139 (04/11/2026)

## Vozes IA por legenda e trilha de voz separada

- Atalho **Vozes IA** no carrossel do editor, entre Áudio e Transição; tela própria com prévia do vídeo e legendas carregadas automaticamente do projeto, somente leitura.
- Consulta manual do catálogo de vozes Gemini, escolha individual por legenda, estilos de interpretação, modelo, aplicação da mesma voz a todas as falas, geração e escuta de WAV antes da inserção.
- Botões **Gerar**, **Refazer**, **Gerar todas**, **Ouvir**, **Inserir/Substituir** e **Excluir faixa**. Confirmação explícita antes de chamadas com possível consumo de cota; lotes internos de até 12 e cancelamento entre chamadas.
- Rascunhos dos WAVs gravados em armazenamento interno durável, com SHA-256 e impressão digital da legenda, para reabrir sem regenerar falas idênticas. Sem transcrever, enviar mídia nem editar legendas originais.
- **Vozes** aparece como faixa própria no editor com gesto para mover e toque para reabrir Vozes IA. Todas as faixas continuam em `audioTracks` para manter a prévia, a mixagem e o exportador anteriores; metadados opcionais distinguem vozes de faixas tradicionais mantendo JSON `schemaVersion=4`.
- Inserção só ocorre após confirmação, sem recortar WAV; alerta quando áudio ultrapassa a duração da legenda ou sobrepõe outra voz; bloqueia inserção fora do projeto.
- Tela Áudio tradicional continua disponível para música, efeitos, extração e narração contínua/avançada. Recurso de imagens na timeline não foi incluído nesta versão.
- O CI/Gradle e a reprodução/exportação reais no Android ainda precisam ser validados. Sem APK produzido neste ambiente.

# Uai Cortes · versão 0.1.138 (04/11/2026)
**Versão atual: 0.1.138**

- CI115: testes de karaokê não fixam mais versionCode/versionName de uma versão histórica; o código de vídeo e legendas permanece intacto.

- Legendas: Desfazer movido para a barra principal, tempo duplicado removido da barra do player e cabeçalho da linha do tempo mais compacto.
- Mudança focada em ergonomia; não altera formato dos projetos nem exportação.


## Novos destaques por palavra nas legendas

- Três opções na categoria Texto: **Pulso de cor**, **Palavra forte** e **Sublinhar palavra**. São variantes do karaokê existente, compatíveis com a sincronização manual da v0.1.134 e horários reconhecidos previamente.
- O pulso é uma **animação de cor quantizada**, sem mudar tamanho ou posição dos caracteres. Palavra forte usa negrito e Sublinhar palavra usa sublinhado apenas na palavra ativa. Três novos modelos prontos somam **25 estilos e 22 animações**.
- `SubtitleAnimations.Frame` descreve o destaque em um único lugar. Prévia Compose, miniaturas de estilos e MP4 Media3 aplicam os mesmos intervalos, cores e opções de negrito/sublinhado; a chave do bitmap inclui a fase do pulso e os atributos do `SubtitleCue` para evitar imagem desatualizada.
- Sem horários individuais, o efeito distribui o tempo por palavra apenas como estimativa e **não altera** o projeto automaticamente. `schemaVersion=4`, IDs, textos, áudio, posição e horários já salvos permanecem intactos.
- Novos testes JVM `SubtitleKaraokeAccentsTest` e contrato Python `test_subtitle_karaoke_136_contract.py`; testes locais não substituem build Android, comparação visual de prévia/MP4 e sincronização real com áudio.

# Uai Cortes · versão 0.1.135 (04/11/2026)

## Correção do workflow 113 — testes de presets de legendas

- O teste `SubtitleStylesTest.presetOnlyChangesAppearance` usava `presets.last()`, que antes era o modelo **Etiqueta**. Após a inclusão de 10 presets na v0.1.133, o último passou a ser **Ritmo rosa**, mas as expectativas de cores do teste permaneceram as de Etiqueta.
- O teste agora localiza **Etiqueta** pelo nome e verifica suas propriedades sem depender da ordem da galeria.
- Nova regressão JVM percorre todos os 22 modelos em ordem inversa e verifica que aplicar aparência não altera ID, texto, horários, posição ou tamanho. Novo contrato `tools/test_ci113_subtitle_preset_contract.py` também detecta testes dependentes da ordem.
- **Sem alteração no código de produção:** os 22 estilos, 19 animações, sincronização por palavra, formato JSON v4 e exportador Media3 permanecem intactos.
- No workflow 113, `compileDebugKotlin`, `compileReleaseKotlin` e `compileDebugAndroidTestKotlin` passaram; `:app:testDebugUnitTest` falhou em 1 de 448 testes e interrompeu o build antes de gerar os cinco APKs. A compilação e execução completa da v0.1.135 requer novo GitHub Actions.

# Uai Cortes · versão 0.1.134 (04/11/2026)

## Legendas · Karaokê com sincronização manual

- Na edição individual, **Sincronizar** permite distribuir tempos por palavra como ponto de partida **explicitamente estimado**, selecionar palavras e ajustar a divisão por 0,1 s ou digitando horários. Não realiza detecção de voz; horários vindos de ASR não são substituídos sem ação.
- Na prévia de 3 s, horários absolutos são escalados para a demonstração mantendo pausas; no modo duração real, mantêm a proporção original. A confirmação ativa o karaokê sem alterar textos, cor, posição ou IDs.
- Limite de edição manual: 40 palavras e duração mínima de 60 ms por palavra. Projetos JSON antigos (`schemaVersion=4`) permanecem compatíveis. O vídeo exportado e a sincronização efetiva com áudio exigem teste Android.

# Uai Cortes · versão 0.1.133 (04/11/2026)

## Legendas: inconsistências corrigidas e efeitos novos

- No modal **Editar todas**, os grupos Estilo, Animação e Posição são independentes; agora há opção separada para copiar cores de **palavras iguais**, sem misturar índices entre frases. As demais palavras mantêm suas cores.
- Texto de confirmação e desfazer descreve os grupos realmente aplicados; sem seleção, o aplicativo orienta o usuário.
- A prévia de animação não inventa texto quando um quadro está vazio; permite **Pausar**, **Reiniciar** e testar o ritmo em **3 s** ou na duração da legenda.
- **Nove efeitos adicionais**: subir, descer, entrar pela esquerda/direita, palavra por palavra, elástico, flutuar, balançar e tremor suave. **Dez estilos prontos adicionais**, totalizando 22. Efeitos organizados em Entrada / Texto / Movimento.
- As curvas de movimento são compartilhadas entre Compose e Media3, com deslocamento horizontal aplicado nas duas saídas. A equivalência visual exata depende de teste em aparelho Android.
- Projetos antigos preservados (`schemaVersion=4`), com novos IDs de efeito opcionais e normalização compatível. Sem serviços externos, sem mudança no motor de mídia.

# Uai Cortes · versão 0.1.132 (04/11/2026)


## Correção do workflow 110 · v0.1.132

- Compilação Debug/Release: import `horizontalScroll` restaurado ao painel Gemini e retorno de `countTokens` marcado com `return@withContext`.
- Contrato CI110 previne regressões; histórico, estimativas, failover e proteção das chaves mantidos. Compilação Android ainda requer validação em CI.

## Monitor Gemini de tokens · v0.1.131

O painel Configurações > Google Gemini > Consumo da IA registra somente consumo **informado pela API** e tentativas locais, com consulta por projeto/modelo e opção Limpar histórico. Antes de descrição, Flow, narração ou legendas, mostra aviso simples e previsão limitada. Em descrição e Flow, `countTokens` é opcional por toque. Isso não oferece acesso ao saldo de créditos nem calcula preços de faturamento automaticamente. Não transmite dados de mídia fora das gerações explicitamente autorizadas. Ver `docs/GEMINI_USAGE_MONITORING.md`.

## Recursos da v0.1.130

- **Múltiplas chaves Gemini:** até oito credenciais AES-GCM/Android Keystore, cada uma com nome, identificador do projeto Google Cloud, seleção de ativa, edição e exclusão confirmada.
- **Alternância opcional:** após erro HTTP 429 RESOURCE_EXHAUSTED, pausa o projeto por dez minutos e tenta, no máximo uma vez por outro projeto identificado e não pausado, sem retentar erro de cobrança, autenticação ou indisponibilidade. Exige consentimento explícito.
- **APIs conectadas:** narração TTS, transcrição de legendas, geração de descrições e refinamento de cenas Flow utilizam o mesmo gerenciador; a operação autorizada pelo usuário não roda em segundo plano.
- **Limites e segurança:** as cotas são por projeto Google Cloud e não por chave; cadastrar duas chaves do mesmo projeto NÃO duplica cotas. O app não cria contas nem projetos e não garante créditos. IDs são informados pelo usuário e não são comprovados automaticamente; usar apenas projetos autorizados, respeitando termos e orçamento. O backup do Android exclui credenciais.
- **Compatibilidade:** chave única anterior preservada; nenhuma modificação nos vídeos, projetos ou sistema Media3. Faltam Gradle/Android CI e teste com projetos reais distintos antes de declarar a alternância operacional.


A Home ganhou três cartões azuis compactos, com ícones e funções independentes: **Novo projeto**, **Animar fotos** e **Prompts Flow**. Os três ficam lado a lado, na mesma linha, sem tela intermediária. O cabeçalho foi reduzido e as edições recentes têm um estado vazio explicativo e um atalho Ver todos quando há projetos. A prévia em Sobre continua reutilizando os mesmos componentes, com cliques desabilitados. Não altera o motor de vídeo, armazenamento, schemaVersion=4, assinatura ou as cinco saídas APK do CI. Build Android e validação visual em aparelho ainda pendentes.

## Versão 0.1.128 · versionCode 129 (04/11/2026)

A exportação agora usa controles compactos com ícones e ações rápidas para destino, 9:16, resolução e compressão. A prévia local foi mantida em painel expansível com guias opcionais para legenda e elementos de interface; há exemplos visuais de Mostrar tudo vs Preencher. O resumo estimado e Salvar ficam no rodapé fixo. Sem alterações no motor Media3, projeto JSON, assinatura ou formatos. Ainda exige build Android e teste físico para confirmar a composição visual.


## Versão 0.1.127 · versionCode 128 (04/11/2026)

- **Correção CI105:** `FlowPromptDraft` deixa de ser `internal` para compatibilidade com a propriedade pública `VideoProject.flowPromptDraft`; o erro do compilador Kotlin `public function exposes its internal parameter type` foi registrado nos logs Debug e Release.
- **Teste preventivo:** `tools/test_ci105_flow_visibility_contract.py` verifica a assinatura pública e a persistência JSON. A execução automática já ocorre no laço de `tools/test_*.py` do GitHub Actions.
- **Sem alterações no editor/exportador:** preservados marca d'água, prompts Flow, media3 1.11.1, schemaVersion 4, assinatura e cinco saídas Release por ABI.
- **Validação:** verificações de código/contratos locais; compilação Android real da versão corrigida e testes físicos permanecem pendentes.

## Versão 0.1.126 · versionCode 127 (04/11/2026)

- **Marca d'água reformulada:** arrastar texto na prévia, nove posições rápidas, botões +/- para tamanho e opacidade (0–100%), guias opcionais, cores, três fontes locais, contorno e sombra. Remover exige confirmação. A posição e aparência seguem o mesmo `SubtitleCue` da prévia/exportação.
- **Modelo de marca reutilizável:** salvar/carregar configurações em preferências locais privadas; nenhum projeto antigo recebe marca automaticamente. Campos opcionais permanecem compatíveis com JSON `schemaVersion=4`.
- **Prompts Flow:** acesso pela tela inicial sem vídeo importado (até 30 conjuntos locais independentes) e pelo editor de vídeo. Estilo-base 3D fixo editável por aparelho, contexto comum e até 12 cenas separadas por conjunto, com modo texto→vídeo ou transformação de referência. Cada cena tem prompt próprio para copiar; configuração 9:16 é feita no Google Flow.
- **Gemini opcional:** apenas após confirmação envia descrição e contexto textual da cena usando a chave local existente; não envia MP4, frames, áudio, URI ou prompt-base. Mostra sugestão para revisão antes de substituir o texto. Não repete chamadas automaticamente.
- **Compatibilidade:** `VideoProject.flowPromptDraft` opcional em projetos e backups JSON, schema 4 preservado. Conjuntos avulsos de Flow e estilos-base permanecem nas preferências do aparelho e não integram backup de vídeos. Media3 1.11.1, 5 APKs Release e demais ferramentas mantidos.
- **Validação:** testes de contrato/estrutura e sintaxe do código; ainda pendentes build Android real, teste visual da marca d'água e chamadas Gemini com chave válida.

## Versão 0.1.125 · versionCode 126 (04/11/2026)

- **Etapa 7 — modelos pessoais de legendas:** a galeria preexistente de 12 estilos foi mantida. Agora é possível salvar uma combinação personalizada de fonte, tamanho, cor, fundo, contorno, sombra, alinhamento, espaçamento, destaque e animação, reutilizável em outros projetos.
- **Biblioteca local:** até 20 modelos com nome único (até 32 caracteres), renomear e excluir com confirmação. Os estilos são armazenados em SharedPreferences privadas com gravação verificada; erros não apagam automaticamente a biblioteca anterior.
- **Aplicação segura:** escolher um modelo altera apenas o rascunho visual; Salvar legenda confirma alterações individuais. O fluxo já existente Editar todas continua aplicando estilo/animação em lote com confirmação e desfazer. Modelos não armazenam nem substituem texto, IDs, tempos, posição ou cores específicas de palavras.
- **Compatibilidade:** não modifica JSON/backup dos projetos (`schemaVersion=4`), Media3 1.11.1, exportador, assinatura, cinco APKs e seleção de redes sociais. A biblioteca é local ao dispositivo, não incluída nos backups de projetos.
- **Testes:** `SubtitleUserPresetTest.kt` e `tools/test_subtitle_user_presets_contract.py` cobrem isolamento de texto, serialização, nomes duplicados e aplicação. O Gradle/Android e a prévia/exportação visual real ainda precisam ser validados em aparelho.

## Versão 0.1.124 · versionCode 125 (04/11/2026)

- **Etapa 6 — perfis sociais opcionais:** Exportar oferece Original, Shorts, Reels e TikTok. Original permanece selecionado por padrão e não modifica o fluxo antigo. Os perfis sociais solicitam resolução fixa 9:16 (720×1280 ou 1080×1920) via `Presentation.createForWidthAndHeight`, preservando MP4 H.264/AAC e o ajuste manual de bitrate.
- **Enquadramento consciente:** Mostrar tudo usa `LAYOUT_SCALE_TO_FIT` (pode gerar barras); Preencher tela usa `LAYOUT_SCALE_TO_FIT_WITH_CROP` (corte central, pode perder conteúdo lateral/superior/inferior). Não distorce a imagem nem realiza rastreamento automático de pessoas. A prévia usa FIT/ZOOM e exibe guias ilustrativas opcionais, não incorporadas ao vídeo.
- **Compressão:** ao escolher perfil social, sugere a taxa Compacta já existente (5 Mb/s em 720p, 10 Mb/s em 1080p); o usuário pode selecionar Alta/Máxima. São parâmetros solicitados ao encoder, não requisitos garantidos de cada rede.
- **Diagnóstico:** `ExportQualityReport` compara largura/altura exatas solicitadas para perfis verticais e mostra a rede selecionada. Preservados PiPs, legendas, marca d'água, áudio, backup, schemaVersion 4 e a exportação sem rede.
- **Testes:** novas regras puras `SocialExportPolicyTest`, contrato `tools/test_social_export_contract.py` e cenários instrumentados de FIT/CROP com fonte 16:9, ainda não executados em Android. Compilação Gradle e fidelidade visual/áudio em aparelho permanecem pendentes.

## Versão 0.1.123 · versionCode 124 (04/11/2026)

- **Etapa 5 — descrição persistente por projeto:** novo `VideoProject.publicationDraft` armazena somente textos, inclusive rascunhos incompletos, em JSON local; exportação/importação do backup preserva automaticamente o campo. Sem migrar schemaVersion 4; projetos antigos leem campo ausente como `null`.
- **Tela "Vídeo salvo":** recupera rascunho anterior, salva alterações automaticamente após 650 ms de inatividade, e sincroniza ao fechar/voltar ao editor. Falhas de armazenamento são apresentadas e impedem fechar com alterações não salvas.
- **Editor → Descrição:** tela independente para revisar, editar, copiar e salvar sem reexportação e sem enviar chamadas Gemini. Cópia continua restrita a descrição + exatamente cinco hashtags distintas, dentro de 100 caracteres, sem emojis.
- **Animar imagens:** ao escolher "Editar com áudio, legendas e PiP", o rascunho digitado é transferido para o projeto criado/atualizado. Ao apenas fechar a exportação avulsa não há projeto ao qual associar o texto.
- Criados `ProjectPublicationDraftTest.kt` e `tools/test_publication_draft_contract.py` para serialização, backups, compatibilidade legada e ligação de UI. Build completo Android e teste no aparelho ainda pendentes.
- Media3 1.11.1, Gemini, arquivos de vídeo, cinco variantes Release e assinatura não foram modificados.

## Versão 0.1.122 · versionCode 123 (04/11/2026)

- **Etapa 4 — descrições com Gemini:** consulta voluntária de modelos via `GET /v1beta/models`, filtrando `supportedGenerationMethods=generateContent` e removendo famílias TTS/live/imagem/embedding. Listagem depende da chave configurada e não realiza geração de conteúdo nem expõe contexto; não garante suporte a JSON até a primeira geração.
- **Modelo selecionável:** campo na tela Vídeo salvo, atualização manual da lista, escolha persistida apenas em preferências privadas locais. Em instalação antiga mantém `gemini-2.5-flash` até o usuário consultar e selecionar outro; nenhuma escolha é imposta sem consultar a chave.
- **JSON estruturado:** `generationConfig.responseJsonSchema` para frase e exatamente cinco hashtags; mantém validação local rígida de 100 caracteres, cinco hashtags distintas e proibição de emojis/emoticons. Respostas inválidas são rejeitadas, sem tentar chamadas adicionais automaticamente.
- **Erros e privacidade:** respostas de rede com tamanho limitado, mensagem contextual por HTTP (401/403, 404, 429, 5xx) sem vazar corpos completos, token, prompts ou chave. Consentimento permanece antes de enviar texto; nunca enviar MP4, quadros, URI ou áudio.
- Incluídos testes `GeminiCaptionModelPolicyTest.kt` e `tools/test_gemini_caption_models_contract.py`. Build Gradle completo, teste com chave real, rede e API Gemini em aparelho **pendentes**; nenhuma cota foi consumida nesta implementação.
- Preservados Media3 1.11.1, schemaVersion 4, cinco APKs Release e assinatura.

## Versão 0.1.121 · versionCode 122 (03/11/2026)

- **Correção do workflow 100:** removido o import top-level `androidx.compose.ui.test.assertExists` em `HomeAboutPreviewAndroidTest.kt`. O método `.assertExists()` permanece nas interações semânticas do Compose; não altera os testes nem a UI.
- Adicionada proteção `tools/test_ci100_compose_import_contract.py`, executada pelo loop de regressão do CI, para impedir a reintrodução desse import inválido nos testes Android.
- Compilação do código principal, os demais avisos de ícones obsoletos e o Media3 1.11.1 não causaram a falha mostrada nos logs. As cinco variantes de Release e a assinatura continuam configuradas, mas ainda exigem um build completo bem-sucedido.
- Sem modificações no exportador, projetos, vídeos, IA, permissões ou `schemaVersion=4`.

## Versão 0.1.120 · versionCode 121 (03/11/2026)

- **Etapa 3 — Media3 estável:** migrados juntos `media3-exoplayer`, `media3-ui`, `media3-transformer`, `media3-effect` e `media3-common` de 1.10.1 para 1.11.1, sem dependências alpha. Mantidos compileSdk/targetSdk 36 e Kotlin/AGP atuais.
- **Medição da velocidade:** tempo real de processamento e tempo de cópia MediaStore medidos separadamente via `SystemClock.elapsedRealtime`. Fator estimado (duração do vídeo / tempo de processamento) aparece apenas quando ambas as medidas são positivas. Não inventar ganho de velocidade.
- **Cópia local mais econômica em chamadas:** `GalleryVideoSaver` usa blocos de 256 KiB em vez de 8 KiB por operação; mesma verificação de bytes, pendência MediaStore e rollback.
- **Cortes rápidos:** não habilitada a otimização experimental indiscriminadamente. O fluxo aplica redimensionamento, bitrate escolhido, áudio, legendas e PiP, frequentemente incompatíveis com trim otimizado. MP4 edit-list também pode reter conteúdo que o usuário pensou ter apagado. É necessário criar futuramente modo de corte simples, elegibilidade restrita e testes em aparelhos antes de usar essa opção.
- **Testes:** contrato Python para versão alinhada, tempos medidos e proteção contra edição experimental; JUnit para exibição de métricas. Compilação completa, lint, smoke Android real e benchmark antes/depois pendentes.
- `schemaVersion=4`, cinco APKs, assinatura persistente, efeitos, legendas, PiP, narração, projetos legados e arquivos originais preservados.

## Versão 0.1.119 · versionCode 120 (03/11/2026)

- **Etapa 2 — fidelidade entre prévia e MP4:** quando há PiP, legendas e marca d'água agora são efeitos da composição inteira, aplicados após o compositor de vídeo (`Composition.Builder.setEffects`), mantendo-os acima das janelas PiP. Sem PiP, permanece o processamento anterior por clipe.
- **Ordem de sobreposição PiP:** normalização da lista de camadas para compensar a regra Media3 (primeira entrada no topo) em relação ao Compose (última camada desenhada no topo), sem alterar a ordem gravada no projeto.
- **Prévia de transições:** zoom cinematográfico, recuo suave e pan usam a mesma curva temporal (`TransitionEffects.motionAt`) na prévia e no `ClipMotionTransitionEffect` do exportador; camadas PiP/legendas permanecem independentes do movimento do vídeo principal.
- **Legendas vazias/sobrepostas:** editor e exportador escolhem a primeira legenda não vazia no instante, evitando que uma legenda em branco esconda outra. Sobreposições válidas ainda requerem inspeção visual.
- **Testes:** JUnit para ordem das camadas, seleção de legenda e curvas de movimento; smoke Android complementar com composição PiP + legenda; verificações estáticas para impedir regressões.
- **Limites:** prévia de efeitos sépia/preto-e-branco e tonalidades ainda é aproximada; compilação Gradle, smoke test Android real e comparação quadro a quadro/áudio continuam pendentes. Não afirmar fidelidade integral sem esses testes.
- `schemaVersion=4`, cinco variantes de APK Release, exportação 720p/1080p, assinatura, imagens, TTS, Gemini e arquivos originais preservados.

## Versão 0.1.118 · versionCode 119 (03/11/2026)

- **Etapa 1 — testes de exportação:** incluído MP4 de teste H.264/AAC de 1,6 segundo (320×180, ~29 KiB) somente nos assets do AndroidTest; fixture real reproduzível e validável por `ffprobe`.
- Incluído `ExportPipelineSmokeAndroidTest`: testa a exportação Media3 com recorte, áudio e legenda, aguarda confirmação de gravação no MediaStore e confere faixas de vídeo/áudio, resolução, duração e abertura do MP4; apaga arquivos temporários e saída de teste ao terminar.
- **CI:** todos os contratos Python `tools/test_*.py` e as verificações de compatibilidade Kotlin/Media3 agora são executados em cada build; `compileDebugAndroidTestKotlin` compila testes instrumentados, mas não os executa.
- **Limite da validação:** testes Python/estrutura executados localmente. Compilação Gradle/Android e execução do teste MP4 em aparelho/emulador ainda pendentes, portanto não afirmar que a exportação real está aprovada.
- Editor, Gemini, MP4 do usuário, schemaVersion=4, chaves e assinatura preservados. Não foi adicionado serviço nem envio de mídia.

## Versão 0.1.117 · versionCode 118 (03/11/2026)

- A confirmação **Vídeo salvo** foi redesenhada: estado compacto, miniatura quando o Android fornecer, nome e qualidade em resumo, pasta de destino, compartilhamento prioritário e opções Abrir/Voltar/Fechar. Informações técnicas ficam recolhidas por padrão.
- Campos **Descrição** e **5 hashtags** editáveis, contador do total (até 100 caracteres com quebra de linha), sem emojis/emoticons, hashtags distintas, validação antes de Copiar ou incluir a legenda no compartilhamento.
- Botão opcional **Gerar descrição com IA** reutiliza a chave Gemini já configurada. Mostra o contexto textual e exige confirmação explícita antes da requisição; envia somente assunto/legendas selecionados, nunca vídeo ou áudio. Não fabrica conteúdo se não houver contexto. Pode consumir cota; sem chave, mantém a edição manual.
- Geração no endpoint `models/gemini-2.5-flash:generateContent` com resposta JSON validada, cinco hashtags exatas e corte conservador da descrição ao limite de 100 caracteres. Erros de rede, cota e formatação informados sem mostrar credenciais.
- Mantido o mesmo diálogo nos fluxos **Exportar vídeo** e **Animar Imagens**, sem alteração de `schemaVersion=4`, MP4, timeline, assinatura ou workflow de cinco APKs.
- **Validação:** contratos locais e testes Kotlin de política de publicação. Ainda pendentes build Android completo, chamadas Gemini reais, teste de miniatura, layout e compartilhamento em aparelhos.

## Versão 0.1.116 · versionCode 117 (03/11/2026)

- Interface **Narração Gemini**: remove dezenas de campos de texto simultâneos e exibe uma única fala editável por vez, com navegação anterior/próxima, preservando os textos e horários originais.
- **Gerar falas/Retomar/Revisar** permanece fixo no rodapé da janela, visível sem percorrer toda a rolagem. Quando desabilitado, explica se falta voz, consulta ao catálogo, texto, espaço disponível ou um lote válido (máximo de 12).
- Importação de legendas paginada em grupos de 12: exibe apenas uma página, com navegação, seleção da página corrente e confirmação manual. Não altera automaticamente os demais trechos.
- **Áudio como referência**: apenas uma fala ativa na análise e nos comandos Ir à legenda/Marcar aqui; Anterior/Próxima substituem os três controles repetidos em cada linha. O processamento continua local e sob demanda.
- Preserva checkpoint SHA-256, cache de WAV, estilos e confirmação antes das requisições ao Gemini. Não altera `schemaVersion=4`.
- **Testes pendentes**: build Android completo, interação real em celular, geração de voz, consumo de cota, análise de MP4 e exportação.

## Versão 0.1.115 · versionCode 116 (03/11/2026)

- Checkpoint privado por projeto para **narração sincronizada**: armazena rascunho de trechos, modelo, voz, estilo, horários corrigidos, metas de duração e caminho/medida/digest SHA-256 de WAVs já gerados. Não registra a chave Gemini e não envia arquivos à rede.
- Após reinício do aplicativo, reabre o lote somente se projeto e horários das legendas ainda forem compatíveis. Cada WAV é verificado contra o digest salvo; ausência, corrupção ou alteração de origem impedem reutilização silenciosa.
- Após cada síntese de um trecho e gravação íntegra do WAV, registra o checkpoint **antes de solicitar o próximo trecho**. Falha/cancelamento pode deixar chamada de API já contabilizada, mas trechos concluídos são reutilizados. Não há novas chamadas automáticas.
- Importador de legendas seleciona até **12 falas por lote**, com botão "Próximas 12"; a ação "Retomar (N)" mostra quantas chamadas faltam e exige autorização. Nenhuma alteração de formato de projeto (`schemaVersion=4`).
- **Pendências de validação:** Gradle/Android completo, morte do processo durante a geração, restauração real de WAVs, transições de lotes e MP4 exportado.

## Versão 0.1.114 · versionCode 115 (03/11/2026)

- Na narração sincronizada, cada WAV preparado tem **Comparar A/B**: ouvir o som original do vídeo na janela correspondente e ouvir o WAV Gemini separadamente. Player local, sem efeito sobre o projeto ou exportação.
- Nova **meta de duração** individual, com ajustes de 250 ms, enviada como orientação opcional de ritmo na regeneração. A API não garante duração exata: sempre comparar com a duração medida do WAV, sem cortar ou fazer time-stretch implícito.
- **Refazer fala** faz uma solicitação Gemini apenas para o segmento escolhido, com confirmação de possível cota. O WAV anterior só deixa de ser usado após novo áudio válido e salvo; falhas preservam os anteriores.
- Diagnóstico de áudio abaixo de 250 ms separado de ultrapassar o fim do projeto, evitando orientação incorreta durante a revisão.
- Mantidas revisão de sobreposição/excesso do vídeo, reutilização das legendas, alinhamento de referência sem upload, schemaVersion 4 e mixagem existente. **Pendências:** compilação Gradle/Android, áudio real da API e teste da prévia/exportação em aparelhos.

## Versão 0.1.113 · versionCode 114 (03/11/2026)

- Referência de áudio agora inclui análise local sob demanda (Android MediaExtractor/MediaCodec) de até 15 segundos em torno da fala selecionada e forma de onda por energia RMS.
- Mostra regiões de atividade sonora e um possível começo próximo da legenda. Música/efeitos também podem produzir picos; não é reconhecimento de fala nem alinhamento por palavra. Sugestões são opcionais e dependem de duas ações explícitas: **Usar sugestão** no rascunho e **Aplicar horários**.
- Processamento limitado em tempo, fora da thread principal, cancelável, sem enviar áudio ao Gemini ou modificar os arquivos originais. Suporta referência do vídeo, faixas existentes e arquivo SAF conforme disponibilidade do decoder.
- `schemaVersion=4` preservado, sem nova transcrição nem geração de voz automática. **Pendente:** compilação Android/CI e testes em aparelhos com mídias reais, formatos variados e vídeos com vários clipes.

## Versão 0.1.112 · versionCode 113 (03/11/2026)

- Na geração por trechos, novo botão **Usar áudio como referência de tempo**: ouvir o vídeo do projeto, uma faixa adicional ou importar áudio local pelo seletor Android.
- Marcação manual dos inícios de falas usando o tempo da referência, com offset ajustável para arquivo externo, revisão e confirmação. Preserva horários/textos das legendas; nenhum processamento de reconhecimento novo ou upload do áudio.
- Os horários de voz confirmados entram na revisão de conflitos e são gravados nas faixas Gemini, mantendo os WAVs já gerados, sem cortar silenciosamente nem mudar `schemaVersion=4`.
- **Limite:** não é alinhamento automático por voz; a API TTS normal recebe apenas texto. Replicação de timbre exige recurso separado, consentimento e autorização; não foi ativada.
- **Pendente:** compilar Android no GitHub Actions e testar referência sonora/Media3 e exportação real em aparelho.

## Versão 0.1.111 · versionCode 112 (03/11/2026)

- **Reutilizar legendas:** modo Sincronizada por trechos ou Contínua. O modo temporal preserva os instantes de cada legenda, permite edição individual sem alterar o texto original e exige confirmação antes das chamadas Gemini.
- Até 12 falas por geração; cada fala usa uma chamada TTS. Em falha/cancelamento, os WAVs já gravados são mantidos e as falas inalteradas não são solicitadas novamente nesta sessão.
- Antes da inserção, revisão mostra a duração real de cada WAV, conflitos com legendas/vozes seguintes e excedentes do fim do vídeo. Excedente final bloqueia a inclusão; sobreposições dentro do vídeo exigem aprovação expressa. Nenhum áudio é cortado automaticamente.
- Faixas sincronizadas são salvas em uma única atualização do projeto após aprovação. Sem mudanças no JSON/schemaVersion 4, nos clipes, legendas ou exportador; a narração contínua permanece disponível e também não é mais truncada silenciosamente.
- **Pendente:** compilação Android completa, teste com Gemini real, sincronização de preview/exportação no aparelho e interrupções do Android.

## Versão 0.1.110 · versionCode 111 (03/11/2026)

- Novo botão fixo **Salvar e voltar ao vídeo** na tela Áudio; persistência é confirmada antes da navegação, impedindo saída quando falha, durante extração ou com corte ainda não aplicado.
- Mixagem com títulos mais legíveis para arquivos de vídeo com nomes numéricos gerados automaticamente, cartões mais compactos e avisos dispensáveis, mantendo volumes e títulos originais intactos no projeto.
- Pré-escuta, edição das faixas, extração e narração Gemini preservadas; esquema do projeto continua versão 4.
- Testes locais e validação de ZIP não substituem build Android nem teste visual/áudio em aparelho.

## Versão 0.1.109 · versionCode 110 (03/11/2026)

- Corrigida a navegação da narração Gemini mostrada no vídeo de teste: confirmar a voz mantém a janela de geração aberta; após consentir com a chamada de IA, uma resposta WAV válida é gravada e adicionada à tela Áudio, sem navegação automática. Falhas de gravação preservam o WAV para nova tentativa sem repetir a síntese.
- Adicionado botão **Ouvir mixagem** no topo da tela Áudio para conferir o som original do vídeo e as faixas sincronizadas. A reprodução não muda o projeto.
- Novo botão de **Volume da prévia** junto ao Play na tela do editor, com ajuste de 0% a 100% para clipes, música, faixas adicionais e áudio dos PiPs. Não altera a mixagem persistida nem o MP4.
- A confirmação de geração esclarece custo/cota e inclusão; removida a solicitação GET desnecessária de validação do modelo, sem repetição automática de chamadas. Preservado schemaVersion 4, cinco APKs por arquitetura e versionCode 110.
- Verificações locais de sintaxe e contratos não substituem compilação Gradle/Android nem chamada Gemini com chave real.

## Versão 0.1.108 · versionCode 109 (03/11/2026)

- A ferramenta **Extrair áudio de vídeo** inicia recolhida, com título e seta para abrir/fechar; mantém origem, pré-escuta, formato e opções Salvar/Salvar e usar. Ao importar um vídeo externo pela galeria, o painel abre para continuar a extração.
- Após gerar uma narração Gemini e confirmar **Adicionar à timeline**, o projeto é persistido antes de voltar ao editor de vídeo. Falha ao salvar mantém o editor de Áudio aberto e informa o problema; não há inclusão silenciosa.
- Confirmar apenas a escolha da voz continua retornando ao editor sem gerar nem adicionar áudio. Salvamento em arquivo externo (Salvar áudio) permanece independente.
- `schemaVersion=4` mantido, versionCode 109, GitHub Manager/Gradle/CI sincronizados. Compilação Android e teste físico pendentes.

## Versão 0.1.107 · versionCode 108 (02/11/2026)

- Corrigido o seletor Gemini para não confirmar uma voz previamente marcada quando ela estiver oculta pelo filtro Femininas/Masculinas ou pela pesquisa. Trocar o filtro limpa uma escolha incompatível; somente uma voz visível pode ser confirmada.
- O círculo de seleção (radio) também responde ao toque, assim como a linha, e o cartão indica claramente a escolha.
- Botão **Ouvir voz marcada** no seletor permite escutar a voz escolhida antes de confirmar. Usa cache local quando disponível; caso contrário exige confirmação explícita de possível consumo de cota antes de chamar o Gemini. A prévia não altera a seleção confirmada, nem cria faixa de áudio.
- Confirmar voz mantém o comportamento anterior: retorna ao editor de vídeo e retoma a prévia sem gerar narração automaticamente. A geração da narração e a inclusão na timeline continuam exigindo confirmações independentes.
- Limite de validação: testes Kotlin isolados e checagens estáticas não garantem timbre feminino no áudio real; requer teste com chave Gemini no Android e build no GitHub Actions.

## Versão 0.1.106 · versionCode 107 (02/11/2026)

- O catálogo de vozes do Gemini foi redesenhado: cartões compactos com nome legível, resumo curto em português, campo de busca e filtros Todas/Femininas/Masculinas baseados somente nos metadados recebidos da API.
- Nome original, ID, descrição em inglês e metadados detalhados aparecem somente em **Detalhes**, com expansão sob demanda; outras vozes continuam acessíveis pela rolagem.
- Ao abrir Escolher voz, a janela de narração fica temporariamente oculta, evitando dois diálogos sobrepostos. Cancelar volta à narração sem aplicar escolha; Confirmar voz retorna ao editor e retoma o vídeo sem sintetizar automaticamente.
- Preservados seleção explícita, cache de amostras, reutilização de legendas, geração com confirmação, atualizações pelo GitHub e ferramentas de Animar Imagens. Teste real no Android pendente.

## Versão 0.1.105 · versionCode 106 (02/11/2026)

- **Animar Imagens:** prévia principal agora decodifica a imagem real via MediaStore/SAF, preservando proporção e orientação EXIF, em vez de usar miniatura quadrada solicitada em 640 px.
- Painel principal mais alto especialmente em 9:16; botão **Ampliar** oferece inspeção maior do enquadramento com o mesmo movimento e composição do editor.
- Seletor **Nitidez:** Rápida (720), Equilibrada (1280) e Alta (1920 pixels no lado maior), sem interpolar imagens originais pequenas e sem modificar opções do MP4 exportado.
- Memória limitada por amostragem, leitura assíncrona e bitmap único por foto; testes estruturais não substituem avaliação física de fluidez e nitidez no aparelho.

## Versão 0.1.104 · versionCode 105 (02/11/2026)

- Em **Atualizações pelo GitHub**, falhas antes ocultas agora aparecem com categorias concretas (DNS, rede, SSL, HTTP e JSON). O app preserva a privacidade: não mostra tokens, dados da requisição ou caminhos internos.
- **Abrir Releases no navegador** permite verificar manualmente o acesso ao repositório configurado. Repositórios privados não são consultáveis anonimamente pela API.
- Atualização automática significa apenas **verificação**; download e instalação continuam sob confirmação.

## Versão 0.1.103 · versionCode 104 (02/11/2026)

- Confirmar uma voz no catálogo agora fecha a janela de narração e retorna à prévia do vídeo no editor, retomando a reprodução no cursor existente.
- O rascunho da narração (texto, voz, catálogo, modelo e estilo) permanece em memória enquanto o mesmo projeto estiver aberto; tocar em confirmar voz não gera áudio nem adiciona trilha ao vídeo.
- Lista de vozes usa linhas clicáveis com botão de seleção, evitando cortes de texto da interface anterior. Nenhuma credencial ou áudio é persistido no rascunho.
- Continua necessário voltar em Áudio → Gerar por IA, confirmar a geração e confirmar a inclusão na timeline para ouvir a voz nova no vídeo. Não há cópia de áudio sem confirmação.
- Testes locais e validações do ZIP não substituem build Android nem teste no aparelho.

## Versão 0.1.102 · versionCode 103 (02/11/2026)

- Diagnóstico explícito na janela Narração com IA: consultar/diagnosticar vozes só por toque, indicar filtro pt-BR, fallback e falhas HTTP/SSL/rede sem mostrar a chave ou legendas. Busca geral após filtro vazio ou rejeição HTTP 400/422; erros de autenticação/cota não disparam retry desnecessário.
- Novos testes automatizados do catálogo com respostas simuladas (execução completa no Gradle pendente; 9 verificações Kotlin isoladas passaram). A consulta não sintetiza áudio nem escolhe voz. Requer compilar no GitHub Actions e testar com a chave real para identificar a causa específica no aparelho.

## Versão 0.1.101 · versionCode 102 (02/11/2026)

- Corrige os dois testes do workflow 85: `HomeAboutVersionContractTest` agora compara Sobre/Novidades com `BuildConfig` em vez de número de versão fixo; a documentação da prévia da Home é buscada no histórico, não apenas no release corrente.
- Testes de sincronização de metadados e verificações estáticas passam a aproveitar os valores atuais do Gradle, evitando versões antigas em asserts. Consulta de atualizações pelo GitHub e confirmações permanecem como na v0.1.100.
- Fonte preparada; a compilação completa Android e o teste real de atualização precisam ser executados no GitHub Actions/aparelho.

## Versão 0.1.100 · versionCode 101 (02/11/2026)

- **Atualizações GitHub Releases**: consulta online ao iniciar o aplicativo ou ao voltar para primeiro plano (com intervalo mínimo de um minuto para evitar excesso de chamadas), inclusive pré-releases, sem download automático. Se houver versão mais recente com APK universal, mostra diálogo; em Configurações → Atualizações pelo GitHub há consulta manual, dados da Release e confirmação para baixar e instalar.
- **Origem confiável**: builds feitos no GitHub Actions recebem `GITHUB_REPOSITORY` automaticamente; builds locais podem configurar um repositório público manualmente. Nenhuma chave GitHub é exigida, não há consulta sem repositório configurado.
- **Segurança**: HTTPS, apenas asset universal oficial, SHA-256 da API, versão superior/mesmo pacote e instalação via Android com confirmação. Certificado precisa ser o mesmo do APK anterior. **CI bloqueia Releases sem os quatro Secrets de assinatura persistente** (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Sem digest não faz download interno.

## Versão 0.1.99 · versionCode 100 (01/11/2026)

- Tela **Sobre**: teste visual da Home com os **mesmos componentes** de Criar projeto e Animar Imagens, mas ações desabilitadas para não alterar dados. Home e Sobre exibem versão pela mesma fonte `AppReleaseNotes`.
- Identificação para o **GitHub Manager**: `github-manager.json`, `app_identity.json` e `VERSION` na raiz. O validador bloqueia versões divergentes antes da publicação.
- Teste Android instrumentado de Home/Sobre adicionado; depende de emulador/aparelho para execução real.

## v0.1.98 (versionCode 99) · 01/11/2026

- **Correção do workflow Gerar APK Uai Cortes 83:** o ZIP da v0.1.97 estava incompleto e não incluía `app/build.gradle.kts`, `build.gradle.kts` e `settings.gradle.kts`. A etapa de validação parava com `FileNotFoundError` antes de compilar Kotlin/Android.
- **Empacotamento corrigido:** os três scripts Gradle retornaram ao pacote de código-fonte. Incluído `tools/package_source.py` para gerar futuros ZIPs, verificar todos os arquivos críticos e validar o conteúdo após extração temporária.
- **Preservação funcional:** importação de legendas com confirmação, escolha de voz, cache, estilos e editor permanecem sem mudanças de funcionamento; `schemaVersion=4` mantido.
- **Versionamento:** `versionName=0.1.98`, `versionCode=99`, workflows e nomenclatura dos cinco APKs sincronizados.
- **Limite da verificação:** o pacote pode ser validado estruturalmente; a execução de `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` ainda depende de ambiente Android/CI, assim como testes em dispositivo.

## v0.1.97 (versionCode 98) · 01/11/2026

- **Áudio → Gerar por IA → Reutilizar legendas**: permite reaproveitar exclusivamente os textos de legendas já salvas no projeto, independentemente da origem offline, Gemini ou manual. Nenhuma nova transcrição, upload ou chamada à API é feita ao importar.
- **Revisão obrigatória**: lista os trechos com horários, permite marcar/desmarcar, mostra quantidade e caracteres. Texto acima de 4.000 caracteres bloqueia a importação, sem cortes silenciosos. A confirmação informa se substituirá o texto editável atual; legendas originais permanecem intactas.
- **Voz manual**: abrir narração não busca catálogo automaticamente; “Consultar” faz a consulta após ação explícita. A lista não escolhe automaticamente a primeira voz; escolher voz exige “Confirmar voz”. Gerar áudio conserva a confirmação de possível consumo da API.
- **Confirmação da timeline**: adicionar o WAV gerado abre diálogo com horário inicial, duração e eventual corte no fim do projeto. A faixa só é incluída depois de confirmar; mídia/legendas originais não são modificadas.
- **Limites**: nesta fase o texto é agrupado em um rascunho de narração; geração e sincronização por trecho/múltiplos falantes continuam planejadas, não implementadas. `schemaVersion=4`; versão `0.1.97`, `versionCode=98`, cinco APKs no CI. Build e teste Android real pendentes.

## v0.1.96 (versionCode 97) · 01/11/2026

- Narração Gemini: 16 estilos de interpretação, mantendo os cinco anteriores, com rótulos em português e instruções em `speech_metadata.style`.
- Ajustes avançados opcionais: ritmo, expressividade emocional e altura/entonação em cinco níveis cada, restauráveis aos valores naturais. Não prometem m/s, Hz ou intensidade acústica exata.
- Uma instrução composta é usada tanto na amostra quanto na geração definitiva; cache considera a instrução completa. Presets antigos com ajustes naturais mantêm a mesma chave de cache da v0.1.95.
- Preserva texto narrado literalmente, autorização explícita antes de solicitar serviço, vozes/IDs, histórico de amostras, edição offline e `schemaVersion=4`.
- Compilação completa Android e validação da fidelidade dos estilos no aparelho ainda pendentes.

## v0.1.95 (versionCode 96) · 01/11/2026

- Catálogo Gemini: nomes traduzidos para expressões conhecidas, preservando nome original e ID técnico; características só aparecem quando fornecidas pela API. Busca cobre tradução, nome, ID e metadados.
- Ouvir amostra: lê primeiro do cache interno, com chave derivada criptograficamente da conta, modelo, voz, estilo e frase fixa. A primeira síntese continua exigindo confirmação e pode consumir cota; reutilizações não fazem nova chamada.
- Cache WAV privado: validação de formato, limite de 24 amostras / 12 MiB, expiração após 30 dias sem uso e limpeza automática possível pelo Android. Amostras não são anexadas aos projetos.
- A API de catálogo não fornece `sample_audio` para vozes predefinidas; não é apropriado assumir acesso direto às prévias existentes no site do Google.
- Não modifica estilos de narração nem a reprodução de legendas; `schemaVersion=4` mantido.

## v0.1.94 (versionCode 95) · 01/11/2026

- **Legendas — reprodução contínua:** selecionando uma legenda, a prévia começa naquele instante, mas o Play não pausa mais em `endMs` da legenda. O Media3 continua pelos clipes seguintes até `STATE_ENDED` ou pausa manual. Ao reproduzir novamente após o fim, começa no início do projeto.
- **Legenda no tempo real:** enquanto reproduz, as legendas exibidas acompanham o relógio da timeline, incluindo legendas seguintes, lacunas sem texto e sobreposições; a seleção de edição permanece independente. Ao pausar, a legenda selecionada volta a ser editável; o botão Reiniciar legenda volta ao início da seleção.
- **Áudio — atalhos sem duplicação:** a primeira seção agora contém Escolher áudio e Gerar por IA lado a lado. Extrair áudio de vídeo mantém sua seção separada e suas opções de origem/formato/salvamento.
- **Compatibilidade:** sem mudanças na API Gemini, vozes, cotas, exportação, mídia ou schemaVersion 4. A v0.1.95 fica reservada para melhorias de catálogo/prévias.
- **Validação:** teste novo para intervalos consecutivos, lacunas, sobreposições e fim. Build Android, lint e ensaio de reprodução real devem ser confirmados via CI/aparelho antes de anunciar APK.

## v0.1.93 (versionCode 94) · 01/11/2026

- **Prévia fixa:** o exemplo animado das legendas fica fora da área rolável dos diálogos Editar legenda e Editar todas; permanece visível enquanto navega pelas opções. A prévia principal da tela de legendas já era fixa acima da lista.
- **Formatação compacta:** negrito, itálico e tamanho ficam em uma única barra; botões de −/+ alteram 2 sp por toque, respeitando 12–48 sp. Presets animados horizontais passam de 158 para 142 dp de largura e 64 dp de quadro.
- **Opções sob demanda:** cores por palavra, posição (horizontal/vertical) e efeitos avançados (fonte, alinhamento, contorno, sombra e espaçamento) recolhíveis. Nenhuma função foi removida.
- **Editar todas:** caixas de seleção verticais substituídas por três chips finitos Estilo/Animação/Posição, com a mesma confirmação e preservação dos textos e tempos.
- **Segurança e compatibilidade:** nenhum campo persistido ou motor de legendas foi alterado; mantém schemaVersion 4. Não mexe em áudio, cache nem na API Gemini.
- **Pendente:** Gradle/Android Lint e validação visual em aparelho; testes locais não confirmam pixels nem exportação MP4.

## v0.1.92 (versionCode 93) · 01/11/2026

- **CI 77:** `:app:lintDebug` reprovou por `Range` em `SubtitleAudioCache.kt:57`, pois `Cursor.getColumnIndex()` pode devolver `-1`.
- **Correção:** o índice de data de modificação passa por verificação `>= 0`, com tratamento de coluna `NULL`, antes da leitura com `Cursor.getLong()`. A ausência de metadados continua usando `-1L`, sem invalidar o cache ou mascarar a falha.
- **Regressão:** `tools/validate_source.py` verifica a condição segura e a ausência de acesso `getLong(getColumnIndex(...))`. Sem desativar Android Lint.
- **Versão:** Gradle, CI dos cinco APKs Release, empacotamento, Sobre/Novidades, README e documentação sincronizados em 0.1.92/93. A v0.1.91 compilou Debug/Release, passou pelos testes unitários e gerou `assembleRelease` nos logs; a publicação foi bloqueada por `lintDebug`.
- **Pendente:** executar novamente CI para confirmar o Lint e publicar os APKs. Nenhum teste físico de legendas/exportação foi efetuado nesta correção.

## v0.1.91 — Galeria horizontal, presets animados e cache de áudio

- Opções visuais de legendas organizadas em rolagem horizontal finita, inclusive cores, fontes, alinhamentos e animações. Galeria de 12 cartões compactos animados.
- Escolha de modelo também define uma animação real; adicionados Bounce, Pulsar e Zoom reverso à prévia e exportação baseadas em SubtitleAnimations.frame.
- Novas transcrições Vosk/Gemini usam tamanho inicial de 20 sp em lugar de 30 sp. Controle 12–30 sp na revisão antes de aplicar; legendas antigas inalteradas.
- Áudio WAV reutilizável em cache privado para chamadas Gemini equivalentes, com hash de clipes/cortes, verificação de comprimento, retenção máxima de 3 horas e limpeza limitada. Nova chamada de API continua necessária.
- Ajuda de posicionamento movida para ícone na linha do tempo; menos texto persistente na tela.
- Build Gradle/Android e testes de exportação e API em dispositivo pendentes.

## v0.1.90 — Ferramentas e IA nas legendas

- Corrigida regressão do `SubtitlePresentationTest`: presets sem configuração explícita de sombra preservam o valor personalizado.
- Barra de ferramentas compacta, cantos de 8 dp, exportação SRT/TXT, e exclusão em lote com confirmação e desfazer.
- Reconhecimento Gemini 3.5 Transcribe opt-in, usando chave já cadastrada; extrai somente áudio temporário dos recortes do projeto, recebe horários por palavra, revisão antes de aplicar.
- Modo offline Vosk preservado; sem envio automático, respeitando limites/cota e `schemaVersion=4`.
- Build completo e testes de aparelho pendentes.

## v0.1.89 — Legendas expressivas e navegação

- Galeria de 12 presets com miniaturas, contorno, sombra, fontes nativas, itálico e cores de destaque.
- Escolha de cor por palavra; texto e cores persistem sem rede e sem afetar mídias originais.
- Novas legendas automáticas do Vosk preservam tempos reais das palavras para karaokê; legendas antigas continuam com aproximação temporal.
- Prévia Compose e MP4 Media3 aplicam a mesma regra de seleção de palavra e as cores de cada trecho.
- Voltar em Animar Imagens leva à Home, mesmo quando aberto pelo editor; mudanças não salvas continuam exigindo confirmação.
- `schemaVersion` 4 inalterado. Compilação Android e fidelidade visual aguardam execução no GitHub Actions/dispositivo.

## v0.1.88 — Salvamento e segurança

- Salvamento de projetos: histórico de cópias locais automáticas com intervalos de 15 s, 30 s, 1, 2 ou 5 minutos, sem duplicar conteúdo e com retenção de 3/5/10 versões.
- Configurações > Salvamento e segurança: escolher frequência, quantidade de cópias, pasta SAF externa, exportar/importar JSON íntegro SHA-256 e confirmar restauração.
- O cadastro do projeto continua salvo a cada edição; cópias históricas são uma proteção adicional. Ao restaurar, o app tenta salvar o estado atual antes de substituí-lo.
- Indicador de mídias não acessíveis ao abrir um projeto. Backup JSON não copia vídeos/fotos/músicas; o MP4 exportado permanece em Filmes/UaiCortes, não Downloads.
- Não foi acrescentado backup em nuvem nem alteração ao esquema JSON 4. A recuperação após limpar dados/desinstalar requer arquivo exportado ou pasta externa acessível.

## v0.1.87 (versionCode 88) · 01/11/2026

- **Animar Imagens:** cinco padrões de câmera (automático, aproximar, afastar, panorâmica horizontal e vertical) aplicados pela mesma função de quadro à prévia e ao Media3; mantendo a opção `auto` em rascunhos antigos.
- **Interface de Animar Imagens:** configurações organizadas em painel inferior rolável; reprodução guiada por tempo real de quadros; toque para inspecionar foto na sequência; cabeçalho “Rascunho” diferenciado de “Gerar MP4”.
- **Prévia:** fundo da própria foto ampliado e escurecido, foto principal sem corte, e transformação sobre toda a composição; ainda é prévia aproximada, sujeita a diferença em transições e hardware.
- **Rascunhos e exportação:** novo campo opcional `motion` no JSON sem perder a leitura dos rascunhos antigos; mantém renderização H.264 offline, duração e formato, sem alterar projetos de vídeo ou `schemaVersion=4`.
- **Validação:** código e testes locais; renderização e comparação visual no Android físico permanecem pendentes.

## v0.1.86 (versionCode 87) · 01/11/2026

- **Correção do workflow Gerar APK 72:** `:app:lintDebug` reprovou com 2 erros `UnsafeOptInUsageError` no `VideoEffects.kt`, ao acessar `TextOverlay.TEXT_SIZE_PIXELS` nos spans de contorno e sombra.
- **Correção localizada:** `@androidx.annotation.OptIn(UnstableApi::class)` nas duas classes, preservando o dimensionamento e estilo das legendas e sem suprimir inspeções do Android Lint.
- **Prevenção de regressão:** verificação estrutural de ambos os opt-ins e da importação correta, com teste específico; metadados de versão e workflow sincronizados.
- **CI 72 confirmado nos logs:** compilação Debug/Release e `:app:assembleRelease` executadas; bloqueio no Lint impediu a publicação dos cinco APKs.
- **Pendente:** repetir o workflow integral de Gradle (`testDebugUnitTest`, `lintDebug`, `assembleRelease`) e verificar geração/publicação dos APKs.

## v0.1.85 (versionCode 86) · 01/11/2026

- **Correção do workflow Gerar APK 71:** `WhatsNewReleaseNotesTest.reorderingOrChangingACaptionChangesTheFingerprint` falhava porque a versão 0.1.84 contém uma única novidade e a lista invertida é igual à original (288 de 289 testes passaram).
- **Teste corrigido, regra de produto preservada:** reordenação agora é verificada com duas novidades artificiais apenas no teste; um novo teste garante que uma novidade isolada tem a mesma impressão digital quando invertida, mas mudanças no texto alteram a impressão digital. Sem adicionar itens artificiais na interface.
- **Confirmado no CI 71:** `compileDebugKotlin` e `compileReleaseKotlin` concluíram com avisos não impeditivos. Publicação dos cinco APKs interrompida nos testes; não houve falha de assinatura identificada.
- **Compatibilidade:** nenhum comportamento do exportador, Media3, Gemini, legendas, PiP ou `schemaVersion 4` alterado. Assinatura e workflow mantidos.
- **Pendente:** executar o workflow completo para confirmar todos os testes, lint e geração de cinco APKs na versão 0.1.85.

## v0.1.84 (versionCode 85) · 01/11/2026

- **Correção do workflow Gerar APK 70:** compilação Debug e Release falharam em `ExportEngine.kt` (linhas 177 e 179) porque `start()` era pública e expunha `ExportBitratePreset` e `ExportQualityReport`, que são tipos internos.
- **Ajuste mínimo:** `ExportEngine.start` passou a `internal fun start`; mantém tipos, parâmetros, callbacks, bitrates, resolução e comportamento de exportação sem mudanças.
- **Regressão:** verificador estrutural agora exige visibilidade compatível nos três símbolos. A validação de sintaxe e o teste isolado do compilador Kotlin não substituem o build Android completo.
- **Preservados:** cinco APKs Release via GitHub Actions, Secrets de assinatura, schemaVersion 4, PiP, Gemini, legendas e projetos.
- **Pendente:** executar workflow para confirmar `compileDebugKotlin`, `compileReleaseKotlin`, testes/lint e publicação dos cinco APKs.

## v0.1.83 (versionCode 84) · 01/11/2026

- **Revisão crítica de v0.1.81 e v0.1.82:** confirmados o uso de `Presentation.createForShortSide`, ordem de redimensionamento antes das legendas e propagação de bitrate. O Media3 pode ignorar parâmetros solicitados; sem testes Android, a melhora visual ainda não está comprovada.
- **Correção de espaço:** a reserva do MP4 temporário e da cópia MediaStore deixa de truncar projetos acima de 24 horas. Cálculos em Long agora saturam em vez de transbordar; estimativa MiB satura em `Int.MAX_VALUE` para durações extremas.
- **Mensagens:** falta de espaço indica o perfil real (Compacta/Alta/Máxima), sem afirmar que toda exportação usa Alta.
- **Regressões:** adicionados testes de duração >24h, saturação e equivalência de reserva para um minuto; preservados os demais 15 testes locais de exportação.
- **Pendente:** teste Gradle/JUnit completo, exportação em aparelho com legendas/PiP, medição de taxa e resolução no MP4, desempenho e comparação após publicação.

## v0.1.82 (versionCode 83) · 01/11/2026

- **Três perfis selecionáveis de bitrate de vídeo:** Compacta, Alta (padrão anterior) e Máxima. Valores solicitados em 720p: 5/8/12 Mb/s; em 1080p: 10/18/28 Mb/s. O encoder pode ajustar o valor e maior bitrate não restaura detalhes perdidos no arquivo-fonte.
- **Estimativas e reserva de espaço coerentes com a escolha:** o tamanho esperado por minuto e o teste preventivo de armazenamento agora usam o perfil selecionado, inclusive na prevenção de falta de espaço para MP4 temporário e cópia na galeria.
- **Aviso de reescala da origem:** consulta dimensões dos clipes em segundo plano e avisa quando o lado menor do vídeo de origem é inferior à resolução escolhida. Não apresenta resolução inventada quando o arquivo não fornece metadados.
- **Tamanho previsto para o projeto:** exibe estimativa aproximada de MiB para a duração total e o perfil selecionado, além da referência por minuto.
- **Quadros-chave:** solicita ao codificador H.264 intervalo de 2 segundos, com fallback do Media3; não afirmar GOP obtido sem verificar o MP4.
- **Relatório de exportação:** diferencia bitrate solicitado e medido, identifica o perfil e fornece nota contextual quando a taxa medida ficar abaixo de metade do alvo. Taxa baixa pode refletir cenas simples, não é diagnóstico definitivo de degradação visual.
- **Compatibilidade:** preserva 720p/1080p pelo lado menor, texto renderizado depois de dimensionar, PiP, legendas, áudio, Gemini, Vosk, `schemaVersion=4`, cinco APKs Release, assinatura e Secrets. Não altera o motor de animações de imagens.
- **Validação pendente:** build Android Gradle e teste físico de MP4 nos três perfis, com legendas e PiP; conferir bitrate e intervalos de quadros-chave no arquivo final antes de afirmar entrega do encoder.

## v0.1.81 (versionCode 82) · 01/11/2026

- **Nitidez de legenda no MP4:** reordena os efeitos do Media3 para normalizar o lado menor do clipe (720 ou 1080 px) ANTES de renderizar legendas e marca d’água, evitando ampliar uma camada de texto rasterizada na resolução menor da fonte. Corrige especialmente exportações verticais 9:16: o modo 1080p passa a solicitar 1080×1920, em vez de cerca de 608×1080. O ajuste de tamanho e posições continua usando o mesmo schema 4. A prévia Compose e o MP4 ainda exigem teste visual no aparelho.
- **Contorno/sombra:** usa tamanho tipográfico consistente com o RelativeSizeSpan no renderizador Media3, evitando dupla multiplicação de escala no desenho dos efeitos.
- **Diagnóstico real do arquivo exportado:** resolução, bitrate de vídeo, quadros por segundo médios estimados, tamanho, codec e alerta quando a altura obtida for diferente da solicitada. Valores indisponíveis são informados como tais, sem estimativa falsa; a confirmação só aparece após o MediaStore salvar.
- **Segurança e estabilidade:** não altera Gemini, PiP, áudio, projeto, assinatura, cinco saídas de APK nem o `schemaVersion=4`. Mantém fallback do encoder para compatibilidade com diferentes aparelhos.
- **Verificações pendentes:** compilação Gradle Android, teste de PiP e legendas em MP4 de 720/1080 e comparação com a publicação nas redes sociais. A imagem publicada não permite diagnosticar sozinha qual etapa gerou a perda de nitidez.

## v0.1.80 (versionCode 81) · 31/10/2026

- **Correção do workflow Gerar APK 67:** falha unitária `SubtitleExportSizingTest.longWordNeverExceedsFrameAndNoNaN` causada por escala mínima de 48% para uma palavra que precisava de redução maior. A tarefa `:app:testDebugUnitTest` interrompia a Release após 270/271 aprovações.
- **Ajuste responsivo de legendas:** `SubtitleAutoFit.factor` agora aplica piso de 48% apenas à redução causada pelo comprimento total da frase; uma palavra isolada pode exigir redução adicional para caber dentro da margem horizontal do quadro, tanto no Compose como no exportador Media3.
- **Escala na exportação:** removido piso absoluto de 0,01 em `SubtitleExportSizing.relativeFontSize`, que poderia reintroduzir corte em casos extremos. Textos, tempos, posição das legendas e esquema de projetos permanecem inalterados.
- **Regressão:** dois novos testes JUnit cobrem palavra longa, limite visual de frases e medidas extremas; execução isolada por runner compatível JUnit: 11/11 aprovados, inclusive o teste que falhou no CI. Compilação Android/Gradle completa ainda pendente.
- **Compatibilidade:** sem alterações nos recursos de PiP, Gemini TTS, Vosk, mediação de áudio, exportação, assinatura ou cinco ABIs. `schemaVersion=4` mantido.

## v0.1.79 (versionCode 80) · 31/10/2026

- **Legendas:** novas legendas iniciam em 92% da altura do quadro (margem inferior de aproximadamente 8%); projeto legado mantém posição individual já salva. Atalho Rodapé · 8% e slider vertical no editor de Estilo, aplicáveis também em lote (posição apenas com confirmação explícita).
- **Textos extensos:** autoajuste do tamanho visual proporcional às larguras medidas do texto e da maior palavra, mirando cerca de 3 linhas sem truncar as falas. A mesma regra é usada na prévia Compose e no bitmap exportado Media3, sem modificar o texto original. Conferir fidelidade de tipografia em aparelho.
- **PiP:** slider 0–100% de opacidade por camada (imagem/vídeo), preservado no JSON esquema 4; prévia TextureView/Compose e exportação via compositor Media3 com alfa. Projetos antigos usam 100%.
- **Captura:** tocar na câmera pausa o player ANTES de fixar o tempo de captura; PNG continua sendo do vídeo original sem PiP/legendas. O indicador mostra o instante capturado e o vídeo permanece pausado.
- **Timeline:** rótulos dos vídeos compactos (Clipe 1, 2…), sem alterar títulos reais dos arquivos nem referências persistidas.
- **Compatibilidade:** exportação, assinatura, cinco APKs do workflow, edição offline, Gemini TTS e schemaVersion 4 preservados. Compilação Android e testes visuais/MP4 reais pendentes.

## v0.1.78 (versionCode 79) · 31/10/2026

- **Legendas → Salvar:** persiste o projeto e retorna ao editor de vídeo do mesmo projeto, não à página inicial. Se a gravação falhar, permanece nas legendas e mostra o erro existente.
- **Editar todas:** ação ao lado do SRT, painel global com abas Estilo/Animação, ajustes avançados recolhidos, confirmação de alteração e botão Desfazer (apenas enquanto o lote atual ainda corresponder ao projeto). Preserva textos, IDs e horários.
- **Edição individual:** mantém texto e tempos e reorganiza os controles visuais; sliders com margens e ações no rodapé do diálogo. Prévia simulada do estilo, com animação do conteúdo e sem mudança de mídia.
- **Reconhecimento Vosk:** relatório do estágio de carregamento do modelo (indeterminado), progresso baseado nos timestamps decodificados por clipe (0–98%), organização dos resultados e cronômetro real. Não estima tempo restante nem inventa progresso durante a abertura do modelo.
- **Compatibilidade:** Vosk offline, SAF/SRT, Media3, Gemini TTS, exportação, assinatura e `schemaVersion=4` preservados. Build Android e validação da aparência/performance em dispositivo físico pendentes.

## v0.1.77 (versionCode 78) · 31/10/2026

- **Configurações:** Gemini é um item clicável igual às demais categorias. Página dedicada para chave criptografada, seis ações em grade de duas colunas, campo com olho e painel de resultado do teste, sem expor a credencial salva.
- **Editor > Áudio:** botão **Gerar por IA** abre um pop-up com editor de narração, modelo, voz, estilo, geração, prévia, player e adição à timeline. O formulário não ocupa mais a tela de áudio.
- **Vozes:** catálogo retornado pela API pesquisável por nome ou ID, sem exibir descrições longas em inglês na tela principal. Os códigos reais seguem intactos na requisição; a prévia pode consumir cota.
- **Compatibilidade:** backend TTS, SAF, Keystore, Media3, projetos `schemaVersion=4`, workflows e assinatura mantidos. Build Android e inspeção visual em aparelho ainda pendentes.

## v0.1.76 (versionCode 77) · 31/10/2026

- **Gemini TTS opcional:** Configurações inclui chave pessoal criptografada via Android Keystore, importação `.env`/TXT/JSON, exportação confirmada, teste real de geração e link para AI Studio.
- **Áudio > Narração com IA:** consulta vozes via `GET /v1beta/voices`, suporta modelos `gemini-3.8-flash-tts` e `gemini-3.8-flash-lite-tts` em Interactions; estilos enviados como `speech_metadata.style`.
- **Integração local:** valida RIFF/WAV, salva áudio em `files/gemini_narrations`, reproduz e insere na faixa `audioTracks` do projeto preservando `schemaVersion=4`. Exportação do MP4 via Media3 existente; WAV aceito como entrada pelo Media3, mas composição real deve ser confirmada em aparelho.
- **Privacidade:** gerações enviam somente o texto/voz/estilo/modelo; os demais recursos permanecem offline. Não insira chaves de desenvolvedor em APK distribuído: use um backend. Áudios locais não são automaticamente apagados ao excluir trilhas ou projetos.
- **Validação:** verificações estáticas e testes JVM; API com chave real, Gradle/Android e exportação em aparelho ainda pendentes.

## v0.1.75 (versionCode 76) · 31/10/2026

- O menu Editar clipe agora abre Áudio do clipe: importar, ouvir, volume e aplicar; preserva vínculos nos cortes, divisões, duplicações e reordenação.
- Preservados schemaVersion=4, trilhas antigas e arquivos originais. Build Android completo pendente.

## v0.1.74 (versionCode 75) · 31/10/2026

- **Tela Áudio redesenhada:** três cartões com Mixagem do projeto, Adicionar áudio e Extrair áudio de vídeo, mantendo seleção SAF, volume original, trilhas, música legada, corte, confirmação de exclusão, pré-escuta e exportação M4A/MP4 AAC.
- **Controles compactos:** ações de origem Projeto inteiro/Clipe atual/Vídeo externo, seleção de galeria e arquivos para vídeo, formatos e salvar/salvar e usar. Rótulos de arquivo resumidos apenas para exibição, sem alterar títulos persistidos.
- **Fluxo real preservado:** ao escolher áudio, o app já o adiciona ao projeto; a tela não mostra botão fictício para aplicá-lo novamente. O seletor de áudio continua SAF, sem copiar URIs transitórias.
- **Compatibilidade:** `schemaVersion=4`, mídias, PiP, legendas, Vosk, captura de quadros e exportação inalterados. Testes Android/CI e aparência no aparelho pendentes.

## v0.1.73 (versionCode 74) · 31/10/2026

- **Velocidade da prévia:** botões − e + percorrem 14 valores entre 0,1× e 6×, com seleção direta ao tocar no valor e restauração para 1×. Apenas prévia; não altera a exportação nem o tempo do projeto.
- **Controles de quadro:** os comandos ±5 s e os quatro botões de ajuste fino foram substituídos por quadro anterior/próximo (busca por tempo aproximado). Eles pausam o player e utilizam FPS nominal do clipe quando disponível, com fallback estimado de 30 FPS.
- **Limitação:** Media3 não garante salto exato entre quadros nem desempenho em 0,1× ou 6× em qualquer aparelho; conferir em testes físicos e com vídeo de FPS variável.
- **Compatibilidade:** botão de câmera PNG, MediaStore, PiP, legendas, formatos de projeto `schemaVersion=4`, Vosk e parâmetros de exportação preservados.
- **Testes:** controles de velocidade e cálculo temporal em JVM, validador e análise sintática; build Android/CI pendente.

## v0.1.72 (versionCode 73) · 31/10/2026

- **Prévia com velocidades extras:** menu compacto em grade, agora de 0,25× até 5× (incluindo 2,5×, 3×, 4× e 5×). A velocidade altera apenas a reprodução, nunca o MP4.
- **Capturar quadro do vídeo:** ícone de câmera ao lado da velocidade extrai o quadro original na posição da timeline, respeitando cortes e limites entre clipes. Salva PNG em `Pictures/UaiCortes` pela galeria do Android 10+.
- **Qualidade e privacidade:** extração local em IO, sem capturar botões da interface; o PNG representa o vídeo de origem, sem PiP, legenda ou transições sobrepostas. Salvar outro quadro não altera o projeto nem os arquivos de vídeo.
- **Falhas seguras:** erro explícito quando a URI perdeu permissão ou o quadro não é extraível; MediaStore apaga entradas parciais; bloqueio de toques repetidos durante a gravação. Em Android 8/9, pasta externa específica do aplicativo é usada.
- **Testes:** novos casos de instante original, cortes, fronteiras, nomes de arquivos e velocidades. Build Android/CI e captura real em aparelho ainda pendentes. `schemaVersion=4` preservado.

## v0.1.71 (versionCode 72) · 31/10/2026

- **Correção de status do Vosk:** a 0.1.70 deixava a última etapa `Descompactando e verificando o modelo...` visível mesmo depois de `installed=true`, devido à mensagem `stage` persistida na tela. Agora o estado transitório é limpo na conclusão e nunca exibido quando o modelo já está instalado.
- **Teste de regressão:** quatro casos JVM distinguem instalação reconhecida, operação em curso, falha e cancelamento. Orientação para testar a transcrição real após instalar.
- **Preservação:** nenhuma alteração no modelo Vosk, arquivos instalados, projetos (`schemaVersion=4`), editor ou exportador. Os três botões continuam na mesma linha quando o modelo não está instalado.
- **Pendente:** confirmação em APK Android e execução real do Vosk nativo; a detecção de arquivos não comprova transcrição funcional.

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

- **Timeline do editor:** criado `TimelinePlaybackCoordinator` independente de Media3/Compose, compartilhando o mapeamento entre tempo global do projeto e posição de cada clipe.
- **Busca consistente:** botões ±5 segundos, ajuste fino, entrada numérica e barra de progresso usam `seekProjectPosition`, inclusive atravessando as fronteiras entre clipes.
- **Arrasto previsível:** o cursor muda imediatamente durante o gesto e a leitura periódica do player não o substitui até o fim do arrasto.
- **Prévia e reprodução:** restaurar posição salva ao preparar mídia após recriação, publicar `isPlaying` pelo listener do Media3 e soltar o player ao sair da tela.
- **Estado pequeno no ViewModel:** `EditorPlaybackViewModel` mantém apenas projeto ativo e velocidade da prévia (0,25×–2×). Não persiste ExoPlayer/arquivos no `SavedStateHandle`, não altera FPS ou velocidade do MP4.
- **Testes:** mapeamento de clipes, limites, overflow e velocidade; build Android/CI e validação no aparelho continuam pendentes.
- **Compatibilidade:** preservados `schemaVersion=4`, opções de exportação 720p/1080p e componentes das versões anteriores.

## v0.1.63 (versionCode 64) · 30/10/2026
- **Animar Imagens modularizado:** `ImageAnimationTaskViewModel` assume o motor Media3, progresso, cancelamento e confirmação do MP4; a interface só apresenta estados e opções.
- **Reaproveitamento:** mesmo `ExportTaskCoordinator` do exportador principal com tickets de tentativas, fases PREPARING/RENDERING/SAVING e progresso numérico somente quando o Transformer o informa.
- **Concorrência:** instância própria de `ImageAnimationEngine` para cada tentativa, para impedir que a limpeza de um preparo cancelado remova arquivos de outra tentativa. A limpeza durante decodificação é adiada até terminar a operação atual.
- **Rotação:** ViewModel mantém a exportação durante recriação da Activity e as opções visuais são salváveis com valores simples; não recupera trabalho após morte do processo.
- **Compatibilidade:** animação continua offline, PNG temporário sem perdas, H.264 720p/1080p, limite de 80 imagens, formato `schemaVersion=4`, integração com editor e cinco APKs por ABI preservados.
- **Pendente:** build Android Gradle/CI, testes de rotação, cancelamento, salvamento e qualidade MP4 em dispositivo físico. Testes unitários da máquina de estados não provam processamento real.

## v0.1.62 (versionCode 63) · 30/10/2026
- **Exportação com ViewModel:** `ExportTaskViewModel` mantém o estado e o motor Media3 durante recriação da tela; a UI apenas exibe progresso e solicita início/cancelamento.
- **Máquina de estados testável:** `ExportTaskCoordinator` controla preparação, renderização e salvamento; tickets por tentativa ignoram callbacks atrasados, e o progresso só aparece quando informado pelo Media3.
- **Salvamento protegido:** cancelamento só é permitido antes de começar a cópia ao MediaStore. O motor conserva o arquivo temporário até a cópia terminar e retorna o resultado na thread principal.
- **Limites:** a exportação não é serviço em segundo plano, não reinicia automaticamente após encerramento do processo e exige validação em Android real.
- **Compatibilidade:** `schemaVersion=4`, parâmetros 720p/1080p, bitrates, recursos e formatos do projeto permanecem inalterados.

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
