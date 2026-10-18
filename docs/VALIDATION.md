# Uai Cortes 0.1.6 | Validação

## Alterações funcionais inspecionadas
- PiP reorganizado em três diálogos personalizados baseados no tema, com posição 3×3, escala 5–90% (atalhos, texto, slider, pinça) e duração início/fim.
- Botão separado para ativar/silenciar PiP em vídeo e controle de volume; `Overlay.muted` serializado com padrão `false` para compatibilidade com projetos antigos.
- Visualização PiP configura ExoPlayer com volume nulo quando silenciado; exportação omite faixa de áudio PiP quando `muted`.
- Seleção PiP pela biblioteca MediaStore e permissão progressiva: READ_EXTERNAL_STORAGE até 32, READ_MEDIA_IMAGES/VIDEO na 33+, READ_MEDIA_VISUAL_USER_SELECTED na 34+.
- Grade lazy de miniaturas em 4 dp, geradas via loadThumbnail (API 29+) e fallback em APIs anteriores, em Dispatchers.IO. Cache LRU de 20 MiB, limite de 3000 itens mais recentes por tipo.
- Botão de gerenciador de documentos do Android preserva seleção SAF como alternativa a permissões, com concessão persistível.

## Verificações locais
- `python3 tools/validate_source.py`: metadados, workflow, integrações de mídia, código PiP e regressões de Media3.
- `kotlinc`: compilar/testar cálculo puro de posições, escala e intervalos com entradas 5%, 10%, 15% e inválidas.
- Inspeção de arquivo AndroidManifest.xml e consistência de release/versionCode.
- Verificação de integridade ZIP do código-fonte.

## Pendências importantes
- Build Android completo (Gradle/SDK 36, CI) e Android Lint.
- Teste em aparelho do acesso total/parcial, pastas (incluindo WhatsApp), miniaturas em versões 26–36 e persistência de permissões.
- Testar memória ao percorrer galerias grandes, prévia e exportação PiP (vídeos com/sem som, proporção e sincronismo).
- Confirmar tamanho exibido e exportado, navegação e diálogos em tema claro, escuro e AMOLED.
- Transição fade/dissolvência ainda não implementada.
