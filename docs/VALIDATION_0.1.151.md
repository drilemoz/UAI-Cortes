# Validação v0.1.151 — versionCode 152
- Verificar `tools/validate_source.py`, contratos Python e testes `GeminiQuotaPolicyTest` no GitHub Actions. 
- Provar médias por projeto/modelo/operação, corte de amostras, 429, dia Pacífico e limites manuais conhecidos; nunca inferir quota do ID do projeto.
- Em aparelho: cadastrar ID real, configurar limites no Consumo da IA, testar atualização da média após 5 gerações com uso retornado pela API, opção sem limite, teclado e rolagem.
- Confirmar que alternância só acontece após 429 com autorização, entre projetos distintos, sem repetir geração concluída e sem confundir 503/401 com cota.
- Compilar :app:testDebugUnitTest, :app:lintDebug, :app:assembleRelease e validar cinco APKs, volumes e temas.
- Nenhuma medição de uso externo/Google pode ser afirmada sem acesso ao AI Studio.
