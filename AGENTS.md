# Regras de manutenção do Uai Cortes
- Atualizar sequencialmente `versionName`, `versionCode`, README, RELEASE_NOTES e nomes do APK em workflow.
- Entregar APK diretamente em Release Assets no formato `UaiCortes-vX.Y.Z.apk` e como Actions Artifact em todo build aprovado. Falhas na publicação devem falhar o job.
- Entregas de código-fonte podem ser ZIP. O download de Artifacts é ZIP empacotado pelo GitHub, mas Releases deve publicar o APK individual (sem ZIP).
- Sem autenticação, IA, publicidade ou serviço de nuvem para edição.
- Não declarar recurso pronto sem implementação e teste. Fade/dissolvência continuam pendentes.
- Nunca adicionar chaves, tokens ou keystores ao repositório.
