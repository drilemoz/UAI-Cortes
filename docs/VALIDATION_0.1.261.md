# Validação 0.1.261 / versionCode 262

- Origem: fonte 0.1.260, log workflow 218 em 09/11/2026; falha Python em `tools/test_quick_split_196_contract.py` antes do build Android.
- Corrigido contrato para navegação com `edit_audio`. O código antigo preserva rotas de divisão e extração.
- Captura do quadro via PixelCopy da região Compose calculada em coordenadas da janela, com mídia pausada, incluindo camadas visíveis. Saída PNG via MediaStore com IS_PENDING e limpeza de erro. Resolução é da área de visualização; testar fidelidade em dispositivo.
- TrimScreen pausa ao atingir o fim e não executa seek automático de volta. Reproduzir novamente reinicia seleção.
- Testes locais devem registrar PASS/FAIL separadamente; Kotlin sintático não substitui Gradle Android.
