# Uai Cortes 0.1.299 — Diagnóstico de engasgos

Base 0.1.298, versionCode 300. Preservados a assinatura, o schema 4 e as cinco variantes do APK.

Em **Exportar vídeo → Analisar engasgos**, escolha o clipe e o MP4 da mesma montagem. Em clipes longos, use o controle de posição para escolher até cinco segundos, sem cruzar a junção. Logo após salvar, o mesmo botão usa o MP4 recém-exportado.

O aplicativo decodifica pequenos mapas de luminosidade, orientados pela rotação do vídeo, e alinha o conteúdo de cada quadro em uma janela de tempo curta. Retornos exigem a reaparição de um quadro anterior seguida de avanço. Repetições consecutivas por conversão de FPS não contam como retornos. Cenas quase paradas, transformações que escondem a origem e incompatibilidade de decodificação podem impedir uma conclusão. Os resultados são estimativas, não uma identificação automática da etapa defeituosa.

O teste isolado usa resolução, compressão e formato da tela de exportação. Usa o mesmo recorte da fonte, sem PiP, transições, trilhas extras ou marca d'água. Reutiliza o ExportEngine, mas a saída é temporária e não entra no MediaStore. Os mapas de luminosidade ficam somente em memória. Quando o relatório local está ativo, ele recebe somente intervalo, ordinal do clipe e contagens ou status.

## Testes

- Testes JVM de ordem de quadros, repetição legítima de FPS, cena estática e janela de clipe recortado.
- Teste instrumentado de leitura H.264 YUV e horários de apresentação da fonte.
- Ensaio de bancada com o comparador Java compilado e quadros decodificados por FFmpeg. Com o original do réu, detectou retornos nos dois MP4s problemáticos (21 e 26 evidências nas respectivas janelas analisadas), mas nenhum no controle reencodado para 30 fps mantendo a ordem visual. Isso valida o comparador, não o decodificador do Android.
- No Android, conferir seleção por galeria e por saída recém-salva, janela no clipe do réu, cancelamento durante o teste e ausência de novo item na galeria. Conferir se a saída sem camadas confirma ou não o padrão. Testar também com foto PiP, fonte com metadados de rotação e vídeo sem movimento.

166 verificações estáticas concluídas sem falhas. Gradle Android não está instalado neste ambiente, então o APK e os novos testes instrumentados não foram executados.
