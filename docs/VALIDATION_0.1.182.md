# Uai Cortes 0.1.182 — Legendas por personagem

## Mudanças
- Campos opcionais `subtitleSpeakers` e `speakerId` com padrão vazio em projetos antigos, sem aumentar `schemaVersion` 4.
- Cores na linha do tempo são só indicadores até a confirmação explícita de aplicação ao texto exportado.
- Zoom virtualizado 1×/2×/4×, prévia compacta e seleção distinta do cue ativo.
- Sem inferência automática de voz ou identidade. Sem transmissão de áudio.

## Pendente em CI/aparelho
Compilar Gradle/Android e Lint; testar diálogo em aparelhos pequenos, lista com 34+ legendas, zoom, projeto legado, exportação Media3, retomada de rascunhos e restauração de backups.
