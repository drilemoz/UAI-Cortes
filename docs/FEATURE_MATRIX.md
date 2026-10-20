## Novidade v0.1.35 (versionCode 36)
- Prévia do vídeo ajustada à orientação/proporção real no editor, Corte, PiP, Legendas e Exportar. Videos mistos mudam a geometria conforme o clipe ativo.
- Legenda longa se adapta ao espaço disponível na prévia, sem alterar seu tamanho salvo nem renderização na exportação.
- Testar visualmente em retrato, paisagem, quadrado, orientação rotacionada e projetos com clipes mistos; build Android pendente.

## Novidade v0.1.34 (versionCode 35)
| Tela Legendas | Situação |
|---|---|
| Busca sem acentos/maiúsculas, resultado filtrado e numeração original | Implementado em código, pendente Android |
| Navegação anterior/próxima diretamente na prévia | Implementado em código, pendente Android |
| Exclusão confirmada e seleção de legenda sobrevivente | Implementado em código, pendente Android |
| Rejeição explícita de SRT acima do limite, sem importar parcial | Implementado em código, pendente Android |
| Nova legenda limitada à duração real do projeto | Implementado em código, pendente Android |

## Novidade v0.1.33 (versionCode 34)
| Tela Legendas | Situação |
|---|---|
| Prévia fixa no topo com ExoPlayer e arrasto da legenda | Implementado no código, pendente Android |
| Timeline horizontal geral virtualizada em janelas de 10s | Implementado no código, pendente Android |
| Lista compacta LazyColumn com editor ativo expansível | Implementado no código, pendente Android |
| Estilos/posição avançada recolhidos | Implementado no código, pendente Android |
| Importar SRT, compatibilidade de horários e schema 4 | Preservados, pendente regressão física |

# Escopo por recurso

| Recurso | No código desta entrega | Validação física |
|---|---|---|
| Tela inicial, Projetos, Ajustes/Geral/Tela/Sobre | Implementado | Pendente |
| Importação vídeos locais e persistência de URI | Implementado | Pendente |
| Adicionar e unir sequencialmente vídeos | Implementado no modelo/Transformer | Pendente |
| Definir início e fim e dividir clipes | Implementado no modelo/Transformer | Pendente |
| Mudar ordem e remover clipes | Implementado | Pendente |
| Prévia dos vídeos com ExoPlayer | Implementado | Pendente |
| Até 4 PiPs, Bottom Sheet inferior com 4 botões na mesma linha, posicionamento anticolisão e áudio individual | Implementado em código, sem build Android confirmado | Pendente |
| Controle de volume e música | Implementado no modelo e processo de composição | Pendente |
| Silenciar primeiro clipe | Implementado | Pendente |
| Corte de faixas adicionais por entrada/saída e posição no projeto | Implementado na v0.1.23, requer teste Android | Pendente |
| Transição corte direto | Implementado | Pendente |
| Fade preto/branco por clipe | Implementado no código, aguardando teste real | Pendente |
| Dissolvência real | Não disponível | Pendente |
| Exportação MP4 720p/1080p e MediaStore | Implementado no motor | Pendente |
| Progresso e cancelamento | Implementado | Pendente |
| Exportação em serviço de primeiro plano | Não | Pendente |

Os recursos novos da versão 0.1.11 ainda **não foram validados por compilação Gradle ou testes físicos**. A execução #3 comprovou compilação Kotlin na versão 0.1.2, que não abrange estas alterações. Manter classificação **alpha de desenvolvimento**.

| Painel PiP compacto, sem alça superior | Implementado no código 0.1.11 | Pendente Android |
| Horários editáveis (busca, início/fim) | Implementado no código 0.1.11 | Pendente Android |
| Sequência de PiPs + confirmação ao faltar tempo | Implementado no código 0.1.11 | Pendente Android |
| Confirmação de sobreposição com persistência por par | Implementado no código 0.1.11 | Pendente Android |

| Cortar e unir: prévia local, miniaturas, RangeSlider e cursor | Implementado no código 0.1.11 | Pendente Android |
| Marcar início/fim e Aplicar corte, desfazer da última aplicação | Implementado no código 0.1.11 | Pendente Android |
| Máscara de horário numérico compartilhada com PiP | Testes Kotlin/JVM locais | Pendente UI Android |

## Atualização de interface v0.1.12 (versionCode 13)
- Editor Cortar e unir simplificado com miniaturas clicáveis sob a prévia, barra de cursor independente e campo Tempo com busca automática por dígitos.
- Marcar fim apresenta aviso claro se o cursor não satisfaz início + 250ms.
- Ações Dividir, Desfazer, Antes, Depois, Excluir na mesma linha; desfazer da última operação de clipes nesta sessão.
- **Estado:** implementado no código; aguarda compilação Android e verificação física.

## Atualização v0.1.13 (versionCode 14)
- Loop infinito nas prévias Cortar e unir (clipe) e editor principal (playlist), até o usuário pausar.
- Seletor de velocidade de prévia em ambas as telas (0,25× a 2×). Velocidade não faz parte da exportação.
- Tutorial rápido logo abaixo dos controles de corte.
- **Pendente:** Gradle, Lint, teste visual, loop de playlist e sincronismo de PiPs em aparelho real.

## Novidades v0.1.14 (versionCode 15)
| Recurso | Código-fonte | Validação em APK |
|---|---|---|
| Alinhamento e retângulos nas faixas | Implementado | Pendente |
| Menu rápido de clipe, excluir, duplicar e volume individual | Implementado | Pendente |
| Legendas manuais e importação SRT | Implementado | Pendente |
| Fade preto/branco por par | Implementado (sem crossfade) | Pendente |
| Extrair áudio M4A/MP4 AAC | Implementado | Pendente |
| WAV/MP3 e dissolvência | Não implementados | Não aplicável |


## v0.1.15 (versionCode 16)
- Correção técnica de compilação: opt-in Material3 nas telas Editor/PiP e captura de largura da timeline. Funcionalidades da v0.1.14 preservadas.

## v0.1.22 (versionCode 23)
| Recurso | Código-fonte | Validação no Android |
|---|---|---|
| Lint Media3: helper com opt-in AndroidX | Implementado | CI pendente |
| APK ARM32, ARM64, x86, x86_64, universal | Gradle/CI configurados | CI pendente |
| Publicar somente APKs Release em GitHub Releases | Workflow configurado | CI pendente |
| Sem novo build em tag v* e sem duplicata em Actions Artifacts | Workflow configurado | Conferir ao publicar |

## v0.1.23: faixas adicionais de áudio

| Recurso | Código | Validação Android |
|---|---|---|
| Entrada/saída em arquivo AAC/MP4 e áudio importado, com posição na timeline | Implementado | Pendente |
| Pré-escuta específica do trecho e cancelar/salvar edição | Implementado | Pendente |
| Persistência de `sourceStartMs` e compatibilidade com JSON antigo | Implementado e testes de unidade | Pendente |
| Aplicar deslocamento de origem na exportação e prévia do editor | Implementado | Pendente |

## v0.1.24: interação visual no áudio

| Recurso | Implementação | Validação |
|---|---|---|
| Arrasto de faixa extra diretamente no editor (ao soltar salva posição) | Código | Teste Android pendente |
| Toque na faixa para editar o áudio selecionado | Código | Teste Android pendente |
| Alças esquerda, central, direita para entrada, posição e saída | Código com lógica isolada e testes | Teste Android pendente |
| Manter mínimo de 250 ms e limites de fonte/projeto | Lógica pura Kotlin | Unitários em CI pendentes |

## v0.1.25: legenda visual e posição temporal
| Recurso | Implementação | Validação |
|---|---|---|
| Posição de legenda relativa à área de vídeo sem dp fixos | Código, âncora Media3 equivalente | Fidelidade com exportação pendente |
| Arrastar legenda sobre quadro de referência | Código, grava ao soltar | Teste Android pendente |
| Ajuste de posição X/Y por 1% | Código e teste puro | Teste de UI pendente |
| Navegar no projeto em passos ±0,1 s/±1 s | Código e teste puro | Teste Android pendente |

## v0.1.26: edição temporal das legendas
| Recurso | Implementação | Validação |
|---|---|---|
| Seleção de legenda por ID na timeline do editor | Código | UI Android pendente |
| Arrastar início, centro e fim em pistas individuais | Código e lógica Kotlin testável | UI Android pendente |
| Persistência de início/fim ao soltar | Código, compatível com JSON v4 | Persistência em aparelho pendente |
| Proteger limites e trechos SRT curtos | Lógica pura Kotlin | Testes JUnit em CI pendentes |

## v0.1.27: personalização visual de legendas
| Recurso | Implementação | Validação |
|---|---|---|
| Cinco estilos rápidos e aparência individual | Código Compose + JSON | UI Android pendente |
| Fundo transparente/escuro/claro e negrito | Prévia Compose e Media3 TextOverlay | Renderização MP4 pendente |
| Copiar estilo para todas com confirmação | Código + lógica isolada | Teste Android pendente |
| Projetos antigos sem campos de estilo | Defaults e testes JUnit | CI e reabertura no aparelho pendentes |

## v0.1.28: fontes e contornos de legendas
| Recurso | Implementação | Validação |
|---|---|---|
| Três fontes locais em cada legenda | Compose + JSON e Media3 TypefaceSpan | Testar renderização Android |
| Intensidade e cor de contorno | Stroke Compose + LineBackgroundSpan no MP4 | Comparar prévia/exportação em 16:9, 9:16 e multilinha |
| Cópia de aparência e projetos antigos | Defaults e testes JUnit novos | Rodar no CI e reabrir projetos antigos |

## v0.1.29: apresentação avançada das legendas
- Sombra diagonal deslocada em 0–6 dp, preta ou branca, sem blur artificial; o valor zero mantém o estilo antigo.
- Espaçamento vertical entre linhas (100–160%) e alinhamento esquerdo/central/direito para textos com múltiplas linhas.
- A prévia usa Compose `Shadow`/`TextStyle`; a exportação usa spans de desenho/alinhamento e exige confirmação de fidelidade em aparelho.
- Leitura segura de campos opcionais e propagação para a ação de copiar aparência em massa.
