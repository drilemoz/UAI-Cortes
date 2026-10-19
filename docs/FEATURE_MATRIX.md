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
| Corte preciso de áudio por trecho arbitrário | Não | Pendente |
| Transição corte direto | Implementado | Pendente |
| Transição fade ou dissolvência | Não | Pendente |
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
