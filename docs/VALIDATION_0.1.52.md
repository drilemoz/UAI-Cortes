# Uai Cortes 0.1.52: matriz de verificação

## Inspeção estática
- Gradle, workflow, empacotador e validador em versionName 0.1.52 / versionCode 53.
- Novos componentes: ImageAnimationDraftStore, ImageAnimationDraftRules, EditorOptionControls, VideoSavedDialog.
- Confirmar manutenção das importações de imagens, animações Media3 e envio à timeline existente.

## Cenários que precisam ser executados em Android
1. Home → Animar Imagens → importar 3 fotos → trocar ordem e qualidade → voltar → escolher **Continuar**; verificar permanência na tela.
2. Mesmo cenário → **Salvar e voltar**; reabrir e conferir imagens, ordem, intensidade, duração, formato, qualidade e transição.
3. Alterar opções → **Descartar alterações**; reabrir e conferir últimos valores efetivamente salvos.
4. Na tela de Animar Imagens, tocar **Salvar** no topo. Confirmar retorno correto à Home ou ao editor de origem.
5. Animar e exportar 720p: verificar abertura da confirmação em tela cheia, reprodução e compartilhamento do MP4.
6. Na confirmação, tocar **Editar com áudio, legendas e PiP**. Verificar criação de um projeto novo quando iniciado pela Home; quando iniciado no editor, conferir a adição do clipe ao projeto atual.
7. Repetir depois de fechar e abrir o app; testar permissões de mídia revogadas e reimportação.
8. Cancelar a exportação antes do salvamento e tentar voltar durante a cópia MediaStore: nenhuma mídia parcial deve ser declarada como salva.
9. Testar projeto antigo (schemaVersion 4), áudio, PiP, legenda e exportação normal sem regressões.

Compilação Android completa e testes físicos ainda **não executados** neste ambiente.
