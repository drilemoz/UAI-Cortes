# Validação do Uai Cortes 0.1.74 / versionCode 75

## Escopo

Redesenho da tela Áudio em três seções: Mixagem do projeto, Adicionar áudio e Extrair áudio de vídeo. Sem alterações nos modelos persistidos, nos motores Media3 ou no schemaVersion=4.

## Preservação dos fluxos

- Volume por clipe, música legada e trilha adicional continuam editáveis, atualizados via `update(VideoProject)`.
- Corte de trilha mantém rascunho, pré-escuta, limites existentes e confirmação via **Salvar corte**.
- Remoção de trilha mantém alerta e não exclui o arquivo físico.
- Adição de áudio continua usando `ActivityResultContracts.OpenDocument` no aplicativo pai: a faixa é aplicada diretamente ao projeto após escolha bem-sucedida, sem botão de ação falso.
- O atalho **Extrair** desloca a rolagem para a seção de extração. A galeria de vídeos externa e o seletor SAF permanecem em ações diferentes.
- Extração continua usando o `AudioExtractionEngine` existente. Formatos de saída M4A/MP4 AAC. Botão **Salvar e usar** requer um projeto com clipe; **Salvar** permite também vídeo externo.
- A tela continua sob a navegação do editor; não foi criada barra inferior global nova porque ela não existe nesse fluxo da aplicação.

## Verificações locais

- `python tools/validate_source.py`: aprovado.
- Parser Kotlin PSI: nenhum erro sintático nos arquivos de produção e testes.
- Kotlin/JVM: seis asserções isoladas de quantidade de faixas e rótulos exibidos, aprovadas.
- Teste estrutural de ZIP: verificar entradas e CRC depois do empacotamento.

## Pendências

- Build completo Android, testes Gradle/JUnit, lint e confirmação visual no aparelho não foram executados neste ambiente.
- Não há confirmação de aparência idêntica ao mock; os controles foram adaptados às ações já implementadas.
