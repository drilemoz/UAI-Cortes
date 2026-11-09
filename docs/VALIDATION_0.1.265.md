# Validação 0.1.265 / versionCode 266

- Problema de origem: PNG 516×917 com bordas da janela e resolução limitada à prévia.
- Captura nova usa FrameSnapshotProject/FrameSnapshotRenderer: transforma um pequeno recorte local com os mesmos efeitos Media3 do exportador, extrai quadro no tempo selecionado e publica apenas PNG.
- Mapeia legendas inclusive word timings e PiPs ao intervalo temporário. Não tenta representar transição entre duas fontes com apenas uma; retorna aviso específico.
- Preserva arquivos originais e projetos, não envia mídia à internet. Após falhas, não publica PNG parcial, remove arquivo temporário na conclusão/cancelamento quando aplicável.
- Testes: FrameSnapshotTiming Kotlin/JVM, contratos Python CI218/265, tools/validate_source.py, ZIP via tools/package_source.py; build Android completo e validação visual/tempo/codificador pendentes.
