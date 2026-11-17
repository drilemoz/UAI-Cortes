# Uai Cortes 0.1.298

Base 0.1.297, versionCode 299. Schema de projeto, assinatura, formato do ZIP e variantes de APK preservados.

## Cobertura

- `EditorDiagnosticsAndPlayheadTest`: conversão da régua para milissegundos, limitação da extensão temporária, destinos de mídia separados e taxonomia sem texto livre.
- `PlayheadRuler298AndroidTest`: régua de 48 dp, toque e arrasto fora da alça e callbacks de início/fim balanceados.
- `ImageClipImportAndroidTest`: verifica a leitura de uma imagem válida, uma corrompida e uma ausente no PiP.
- Conferência manual no aparelho: arrastar pausado e durante reprodução, usar o zoom, tocar no fim, alternar entre a régua e as alças dos clipes, verificar a hora junto ao cursor.
- Com diagnóstico ativado, usar uma imagem de PiP ilegível e outra inválida. O relatório diferencia `preview_open` e `preview_decode` e limita eventos idênticos repetidos a um por 10 s. Importações distinguem `main_image`, `pip_image` e `pip_video` quando o tipo é conhecido.
- Forçar falhas na prévia de uma junção e conferir `plan_failed`, `frame_prep_failed`, `render_failed` ou `play_failed`. A prévia registra a junção e o efeito selecionados. A falha de um quadro identifica a junção efetivamente processada. Falha genérica da codificação não atribui uma junção específica.

Os eventos não recolhem nomes, URIs, nomes de projeto, texto, fotos ou quadros. A validação física da interação e da reprodução requer Android. O relatório antigo não é alterado retroativamente.

## Resultado local

165 verificações estáticas passaram. O ambiente não dispõe de Gradle Android, portanto a compilação e os testes Android não foram executados aqui.
