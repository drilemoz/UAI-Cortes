# Uai Cortes 0.1.82 / versionCode 83 — melhorias na exportação

## Base e objetivo

- Base: ZIP da versão 0.1.81 recebido na conversa.
- Regras: AGENTS.md do repositório; manutenção de cinco APKs Release, assinatura existente e schemaVersion 4.
- Objetivo: dar controle real de bitrate ao usuário e permitir interpretar o resultado sem prometer qualidade impossível.

## Implementação

1. Três perfis de bitrate H.264 na tela Exportar: Compacta, Alta (padrão compatível com 0.1.81) e Máxima.
   - Lado menor 720: 5, 8 e 12 Mb/s, respectivamente.
   - Lado menor 1080: 10, 18 e 28 Mb/s, respectivamente.
   - Parâmetros são solicitados ao Media3, que pode ajustá-los. Áudio AAC continua em 192 kb/s solicitado.
2. Seleção propagada pela UI, ExportTaskViewModel e ExportEngine; estimativa de espaço e reserva preventiva usam o mesmo perfil.
3. Solicitação de quadro-chave a cada 2 segundos usando VideoEncoderSettings.Builder.setiFrameIntervalSeconds(2f), sem desligar o fallback do Media3.
4. Relatório posterior ao salvamento com bitrate solicitado e bitrate médio medido separados; nota contextual para diferença grande. Bitrate menor pode vir da complexidade baixa do conteúdo.
5. Alerta não bloqueante quando uma fonte oferece resolução menor que a saída; metadados lidos localmente por MediaMetadataRetriever em Dispatchers.IO.
6. Estimativa aproximada do tamanho para a duração total do projeto; texto explicativo recolhido para preservar layout compacto.
7. Preservados: reescala pelo lado menor antes de rasterizar legendas, PiP, áudio, TTS, Vosk, projetos anteriores, fluxo MediaStore e as cinco variantes do workflow.

## Testes executados

- `python3 tools/validate_source.py`: aprovado, incluindo verificações de encadeamento dos perfis UI → ViewModel → motor → encoder/armazenamento.
- Testes JVM dos arquivos `VideoExportQualityTest.kt`, `ExportQualityReportTest.kt` e `ExportSourceQualityPolicyTest.kt`: **15 aprovados, 0 falhas**, compilados com `kotlinc` e executados por runner local compatível com as asserções JUnit usadas nesses três arquivos. Não equivale a `gradle test`.
- Parsing Kotlin via Kotlin PSI: **178 arquivos, sem erros sintáticos**. Parsing não resolve dependências Android nem faz validação de tipos completa.
- XML: **6 arquivos válidos**; workflow YAML: sintaticamente válido.
- ZIP final: validar contagem de arquivos e CRC no empacotamento.

## Pendente / limitações

- A distribuição Gradle não está instalada neste ambiente e o wrapper do projeto não contém `gradle-wrapper.jar`: **sem compilação Android, APK e testes instrumentados**.
- Falta rodar no GitHub Actions `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease`, verificar assinatura e as cinco saídas APK.
- Falta conferir exportações reais 720p/1080p nos três perfis, GOP obtido, comportamento térmico, custo em armazenamento, visual das legendas/PiP e reprodução do MP4 em outros aparelhos.
- Aumento de bitrate não recupera resolução ou detalhes de fontes comprimidas; redes sociais podem recomprimir.
- Não houve teste de upload de mídia ou geração Gemini nesta alteração; nada novo envia vídeo à nuvem.

## Referências oficiais consultadas

- AndroidX Media3 `VideoEncoderSettings.Builder`: https://developer.android.com/reference/kotlin/androidx/media3/transformer/VideoEncoderSettings.Builder
- AndroidX Media3 `DefaultEncoderFactory.Builder`: https://developer.android.com/reference/androidx/media3/transformer/DefaultEncoderFactory.Builder

## Próxima versão sequencial

0.1.83 / versionCode 84, se houver correções após build e testes de exportação em aparelho.
