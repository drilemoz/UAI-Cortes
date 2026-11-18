# Uai Cortes 0.1.308 — Exportação de legendas

Base v0.1.307. versionCode 309. Sem mudança no formato interno do projeto.

- O arquivo .srt.txt relatado contém seis blocos SubRip numerados e temporizados. O nome foi reclassificado pelo destino porque os seletores antigo de SRT e TXT usavam o mesmo MIME text/plain. A correção separa os tipos de documento.
- SRT solicita application/x-subrip. TXT solicita text/plain. Ambos são gravados em UTF-8 com número, início, fim, milissegundos e texto. A importação reconhece os horários pelo conteúdo. O texto não contém estilos, animações ou tempos por palavra.
- O seletor pode usar provedores locais e de nuvem. O app informa a extensão realmente gravada quando o provedor permite consultar o nome e não presume o nome nesse caso. A opção SRT direto em Downloads, no Android 10+, grava via MediaStore em Downloads/UaiCortes e verifica a extensão antes de publicar. Se o arquivo falhar, remove a entrada pendente.
- O HTTP 503 recebido ao revisar com Gemini indica indisponibilidade da chamada. O usuário pode optar por nova tentativa, pelo outro modelo ou por copiar o bloco com horários. O aplicativo não faz chamada adicional por conta própria e não altera automaticamente a legenda.

171 scripts locais passaram, a versão foi sincronizada e o ZIP validado. O anexo foi conferido quanto à estrutura e copiado byte a byte com extensão .srt, sem mudar texto nem horários. Não foi possível gerar APK ou rodar JUnit/Android neste ambiente. Confirmar no aparelho o nome nos dois destinos, a reimportação dos horários e o comportamento do Gemini em rede.

Referências técnicas: documentação Android ACTION_CREATE_DOCUMENT e MediaStore.Downloads, e guia oficial de erros da API Gemini.
