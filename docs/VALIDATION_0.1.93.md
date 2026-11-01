# Uai Cortes v0.1.93 — controles compactos e prévia fixa

## Evidência e mudança
- Inspeção de vídeo de 77,5s com navegação por Estilo/Animação, configurações individuais e Editar todas. Prévia desaparecia ao rolar.
- Prévia animada movida da coluna rolável para o topo fixo dos dois diálogos.
- Barra de controles básicos B/I/tamanho; grupos recolhíveis Palavras, Posição e Avançado.
- Chips finitos da edição em lote; confirmação e preservação de tempos/textos mantidas.
- Apenas layout Compose modificado, schemaVersion=4 inalterado.

## Validação
- Validador estrutural do projeto e verificação da ordem prévia/verticalScroll em ambos os diálogos.
- Parser sintático Kotlin em arquivos do projeto; não equivale à compilação/tipagem Android.
- APK, Android Lint e testes de pixels, scroll e exportação de vídeo em aparelho pendentes.
