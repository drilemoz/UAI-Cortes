# Validação — Uai Cortes 0.1.185

## Alvo
Simplificar Vosk/Gemini e a seleção manual de personagens sem alterar transcrição, custos, rascunhos nem persistência.

## Regressão
- Contrato novo `tools/test_subtitle_compact_help_185_contract.py` verifica acesso aos detalhes, avisos de privacidade/custo e atribuição explícita.
- Executar `python3 tools/validate_source.py`, `python3 tools/check_version_sync.py` e todos `tools/test_*.py`.
- Análise de sintaxe Kotlin dos componentes alterados.

## CI/Android obrigatório
- Compilar `:app:assembleDebug`, executar `:app:testDebugUnitTest`, `:app:lintDebug` e montar os cinco APKs Release no GitHub Actions.
- Confirmar diálogos em fontes grandes, tela pequena, 0/1/16 personagens, rotação e modo escuro.
- Verificar que Gemini exige confirmação e pode exibir estimativas expandidas, com texto rolável, mantendo Cancelar/Enviar sempre acessíveis.
- Validar Offline Vosk sem rede e a recuperação do checkpoint Gemini após uma interrupção.
- Verificar TalkBack dos ícones `?` e dos botões de diálogo.

## Limites
Nenhuma chamada Gemini nem APK Media3 foi testado localmente. Contratos não substituem compilação/uso físico.
