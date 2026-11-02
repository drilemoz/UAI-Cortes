# Validação v0.1.104

- Código-fonte da v0.1.103 foi a base, sem alteração das mídias/edição.
- Captura informa erro genérico de conexão em `drilemoz/UAI-Cortes`; a origem do erro não é comprovável só com essa captura.
- O tratamento anterior capturava toda Exception com a mesma mensagem. Agora o resultado expõe apenas categoria de falha e HTTP, sem mensagem bruta da exceção.
- Incluídos testes com UnknownHostException, SocketTimeoutException, SSLHandshakeException, ConnectException, JSONException e respostas HTTP (401/403/404/429/5xx), com cenário de privacidade.
- Acesso manual por navegador usa somente URL HTTPS do repositório normalizado.
- Pendente: compilação Android completa, verificação de conectividade e Release publicada em aparelho real, instalação com mesma assinatura.
