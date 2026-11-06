# Uai Cortes v0.1.196 — Validação da ferramenta Dividir vídeo

## Mudanças
- Acesso direto na Home: Dividir vídeo e Extrair áudio.
- Partes calculadas em milissegundos, preservando o final; limites 9,5/10 s e 180 partes.
- Media3 Transformer processa uma parte por vez, com saída separada e rollback do MediaStore.
- Opções manter/retirar áudio; extrair M4As individualmente em Android 10+.

## Testes exigidos no Android/CI
- Vídeo Flow curto (8s), 28s e múltiplos de 9,5/10s; medir duração real dos arquivos, quadro de junção, A/V sincronizado.
- MP4 com codec/áudio distintos (H.264/AAC, H.265 etc.), fontes sem áudio; formato retrato e paisagem.
- Cancelamento durante codificação, cópia e extração M4A; garantir que arquivos parciais não aparecem na galeria.
- Falta de espaço, arquivo removido, indisponibilidade do encoder e multitarefa.
- Android 10–16 para M4A; Android 8/9 precisam validação de comportamento do MediaStore legado (não alegar compatibilidade de publicação automática).
- Verificar que não há alterações em projetos nem mídia de origem; testar regressões de Exportar, Narrador, Flow e Central de Vozes.

Testes estáticos/Kotlin isolados não substituem build Gradle nem teste de mídia no hardware.
