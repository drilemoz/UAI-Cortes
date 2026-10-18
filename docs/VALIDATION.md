# Uai Cortes 0.1.9 | Validação

## Funcionalidade implementada no código
- Persistência de até quatro PiPs no campo `overlays`, `schemaVersion=2`, IDs estáveis; projetos com `pip` legado migrados em leitura.
- Quatro botões quadrados na mesma linha, Bottom Sheet ancorado embaixo, sobreposição dos controles sem deslocamento da prévia, nove atalhos, barra/entrada percentual, duração e mídia.
- Adição e substituição via MediaStore/SAF, controles independentes de volume e de silêncio.
- Layout anticolisão usa aspecto da mídia e do quadro, horário de início/fim e prioridade do PiP editado. Rejeita layouts impossíveis com Toast.
- Timeline individual por PiP e composição Media3 com entradas de vídeo ordenadas antes do vídeo base e faixas de áudio independentes.

## Testes disponíveis
- Parser Kotlin PSI: sintaxe dos arquivos Kotlin, não valida tipos/dependências Android.
- Smoke test Kotlin/JVM de geometria: quatro PiPs simultâneos, separação, reutilização temporal, bloqueio de layouts impossíveis e limite quatro.
- Testes JUnit adicionados para colisões, priorização, limites e migração JSON.
- `python3 tools/validate_source.py`: versão, workflow, referências a múltiplos PiPs e remoção dos diálogos PiP.
- ZIP: validação CRC, presença de fontes e documentação.

## Pendências antes de considerar validado
- Executar workflow `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` e verificar APK/assinatura.
- Testar física e visualmente: animação de expansão sem corte, gestos, layouts com PiPs grandes ou vídeos verticais, Tema Escuro/AMOLED.
- Testar prévia/exportação com 1, 2, 3 e 4 PiPs em vídeo; H.264/H.265, áudios opcionais, posições, proporção e sincronismo.
- A disponibilidade de 4 decodificadores simultâneos depende do hardware. Não afirmar compatibilidade antes de medir.
- Confirmar permissões de galerias (parcial e total) e URI SAF persistente após reiniciar.
