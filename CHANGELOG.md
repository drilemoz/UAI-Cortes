## 0.1.9 (versionCode 10) — 18/10/2026
- Reorganizadas as ferramentas Posição, Tamanho, Duração e Mídia em **uma única linha** com quatro botões quadrados compactos e ícones.
- Os ajustes agora abrem em **ModalBottomSheet** na borda inferior; aparecem sobre os controles, sem empurrar o vídeo. Altura limitada, rolagem interna, tema do app e fechamento por gesto, toque fora ou botão.
- Mantidos os controles completos (9 posições, tamanho 5–90%/manual, duração e mídia), seleção de até quatro PiPs, áudio independente, anticolisão e compatibilidade de projetos.
- Sincronizados Gradle, tela Sobre, workflow de APK, README, AGENTS, release e validações. Compilação Android e testes físicos pendentes.

## 0.1.8 (versionCode 9) — 18/10/2026
- PiPs múltiplos (até 4) em um projeto, com seleção e edição independentes.
- Interface quadrada expansível na própria tela (Posição/Tamanho/Duração/Mídia); removidos os pop-ups de configuração.
- Motor de colisões espacial e temporal com prioridade para o PiP editado e aviso quando não houver espaço livre.
- Linha do tempo dividida em uma faixa por PiP, posição de início e duração corretas.
- Prévia e composição Media3 das até quatro fontes visuais; áudio independente para cada vídeo PiP.
- Modelo JSON `overlays` e `schemaVersion=2`; leitura automática do campo `pip` dos projetos antigos.
- Documentação/workflow atualizados para 0.1.8 e versionCode 9; APK ainda depende de compilação CI.

## 0.1.7 (versionCode 8) — 18/10/2026
- Corrigida compilação Debug/Release relatada no workflow 7: `PrimaryTabRow` do Material 3 requer `@OptIn(ExperimentalMaterial3Api::class)` na `DeviceMediaBrowser`.
- Preservadas biblioteca de mídia PiP, controles de posição/tamanho/duração e áudio individual do PiP.
- Versionamento e publicação APK sincronizados (Gradle, tela Sobre, workflow, README, release, validador).
- Ainda requer validação de build Android completo no GitHub Actions e testes físicos.

## 0.1.6 (versionCode 7) — 18/10/2026
- Menu compacto para PiP: janelas de Posição (9 atalhos), Tamanho (5% a 90%, slider e entrada manual) e Duração (início/fim e faixa).
- Nova biblioteca local de mídia: abas Imagens/Vídeos, filtros por pasta, miniaturas em grade com espaço reduzido, ícone e tempo dos vídeos. Usa MediaStore, permissões Android e alternativa pelo seletor de arquivos.
- Áudio de vídeo PiP em cartão próprio: silenciar/ativar sem perder volume escolhido; o estado é persistido e respeitado na prévia e na exportação.
- Ajustes de geometria para 5%, testes para entrada manual e serialização do estado mudo, documentação e workflow sequenciais.
- Compilação Android e verificação em aparelho pendentes.

## 0.1.5 (versionCode 6) — 18/10/2026
- PiP com visualização do vídeo principal, posição rápida 3×3, exibição proporcional e tempos de início/fim.
- Corrigida a posição central e limites da área de arrasto, guias centrais e prévia reproduzível.
- Adicionados testes Kotlin da geometria e da janela temporal.
- Workflow e documentação atualizados para UaiCortes-v0.1.5.apk.

# Histórico de versões

## 0.1.3 (versionCode 4)
- Opt-in pontual do Media3 UnstableApi no indicador de buffering da prévia, corrigindo falha Lint #3; sem ocultar erros do Lint.

## 0.1.2 (versionCode 3)
- Correção dos imports Media3 no motor de exportação e validador estático.

## 0.1.1 (versionCode 2)
- Atualização do setup Android para remover pacote SDK legado.

## 0.1.0 (versionCode 1)
- Base nativa do Uai Cortes e telas iniciais do editor.
