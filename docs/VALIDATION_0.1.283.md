# Validação da v0.1.283

- O padrão 14 é aplicado a novas legendas. Os valores salvos continuam sendo lidos. O ajuste manual permanece.
- A revisão de SRT casa a frase exata (desconsiderando acentos, pontuação e caixa), num intervalo próximo ao SRT. Correspondências ambíguas ficam intactas. Não reescreve a fala nem cria novas sobreposições para pares originalmente separados.
- A referência pode ser a faixa principal ou um áudio/narração já presente. A IA online envia a fonte selecionada só mediante ação explícita. Vosk é local e requer modelo instalado. Os motores existentes aceitam até 10 minutos por análise.
- A prévia em tela cheia renderiza o projeto inteiro no cache privado com o mesmo pipeline do exportador. Não salva na galeria. Usa a resolução padrão configurada e permite Original ou Vertical com ajuste de enquadramento padrão.
- Testes JVM cobrem palavras, horários, cortes originais e valor salvo. No aparelho, conferir a posição de reprodução, saída limpa, primeiro texto em sobreposição e o estilo do karaokê no MP4.

```sh
gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.uaicortes.SubtitlePrivateExportPreviewAndroidTest
```
