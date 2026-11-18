# Uai Cortes 0.1.304 — Vozes e Legendas

Base 0.1.303. versionCode 305. Nenhuma mudança no formato dos projetos.

## Vozes IA

- Prévia, inserção de uma fala e inserção em lote têm preferências independentes. O teste de voz de personagem compartilha a preferência de prévia.
- A preferência só muda quando a ação marcada é confirmada. Próximos toques explícitos usam a mesma validação e o mesmo caminho de execução. O cache de prévias continua prioritário. Nenhuma geração é disparada ao abrir a tela.
- As confirmações podem ser restauradas nas configurações do Gemini. Exclusões e a geração de falas do projeto mantêm suas confirmações.
- Na inserção Gemini por fala/lote e na clonagem inserida em Vozes IA, o volume salvo do vídeo principal fica em até 10% quando o trecho do clipe intersecta a faixa gerada. Vale para o clipe inteiro. Volumes inferiores são preservados. Vídeos de outras partes, fotos, música, PiP, legendas, arquivos originais e as demais faixas de áudio não são modificados por essa regra.
- O volume pode ser alterado novamente na edição do clipe. Só remover a voz não restaura o volume. Alertas de possível dessincronia ou sobreposição entre duas vozes geradas continuam no retorno da ação quando a confirmação é dispensada.

## Legendas

- A lista usa páginas de cinco. Busca, filtros, timeline, editar todas, apagar todas e exportar não ficam limitados à página.
- A faixa mostra importar SRT, exportar, editar todas, estilos por personagem e apagar todas. Os ícones têm descrição, rótulo ao manter pressionado e alvos de 48 dp. A lixeira continua com confirmação e opção de desfazer na sessão.
- [Prévia de interface](Previa-Legendas-UaiCortes.png) com dados fictícios. Não é captura de Android.

## Verificação

167 scripts de regressão Python passaram, além de validate_source.py, CI70/CI72 estáticos e sincronização de versão. Os testes JUnit novos cobrem grupos de cinco e volume no limite de corte, fotos e redução idempotente. JUnit, compilação e lint não foram executados porque a distribuição Gradle e o wrapper JAR não estão disponíveis neste ambiente.

No Android, verificar um projeto com mais de cinco legendas, busca e seleção na timeline, duas falas cruzando um corte e exportação com música independente. Testar a opção de ocultar com Cancelar e com Confirmar, fechar e reabrir a tela e restaurar as confirmações pela engrenagem. A prévia já armazenada não deve provocar nova requisição.
