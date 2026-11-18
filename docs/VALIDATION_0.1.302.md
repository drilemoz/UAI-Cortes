# Uai Cortes 0.1.302 — Polly

Base 0.1.301, versionCode 303.

O HTTP 400 na listagem de vozes não identificava a causa. O app agora usa o pedido mínimo `GET /v1/voices?LanguageCode=pt-BR` e acrescenta somente um token de página válido quando fornecido. A retirada do parâmetro opcional evita diferenças de interpretação, mas não comprova que ele causou o 400 específico.

O tratamento de erro consulta `x-amzn-errortype` e tipos de erro estruturados em um corpo limitado. Exibe apenas códigos permitidos e mensagens próprias em português. Corpo bruto, cabeçalho de autorização e credenciais não são enviados a logs, nem exibidos. Teste de voz continua exigindo confirmação e não repete a requisição automaticamente.

Os testes cobrem filtro, paginação, desconhecidos, negação de acesso, token e data/hora. Nenhuma credencial de usuário foi usada e não houve chamada AWS autenticada.

169 verificações estáticas passaram. A compilação Android e os testes JVM via Gradle não foram executados neste ambiente.
