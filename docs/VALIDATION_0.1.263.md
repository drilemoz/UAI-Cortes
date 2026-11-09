# Validação 0.1.263 / versionCode 264

- Origem: UaiCortes v0.1.262 (código-fonte). Pedido do usuário: cursor acompanhar linha do tempo do Editor de Áudio; escolher vídeo para extrair/editar o som em seguida.
- Áudio e vídeo são selecionados via OpenDocument, com permissões de leitura. O MediaExtractor escolhe a faixa de áudio; o player desliga a trilha visual para não gastar recursos exibindo vídeo.
- Visualização desenha linha de playhead sobre onda real; posição usa ExoPlayer.currentPosition. Toque na onda faz seek limitado a início/fim do trecho; alças mantêm seleção, reprodução e exportação.
- Exportador já possuía setRemoveVideo(true); continua criando AAC/M4A somente áudio via Media3 sem alteração do original, sem VPS.
- Testes: validar política Kotlin/JVM e contratos de integração, validação estrutural, sintaxe, integridade do ZIP oficial. Compilação Android completa, teste de codecs de vídeos reais e feedback de toque/cursor dependem de CI/aparelho.
- Contrato legado `test_ci245_updates_subtitles_export_contract.py` passou a aceitar o rótulo responsivo da v0.1.262; controles de download anteriores preservados.
