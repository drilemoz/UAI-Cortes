# Uai Cortes 0.1.301 — Configuração do Polly

Base 0.1.300, versionCode 302. Schema, assinatura e variantes de APK preservados.

## Como usar

- Preencha ID da chave de acesso, chave de acesso secreta e região. Token de sessão só para credenciais temporárias.
- **Testar conexão** autentica e lista as vozes pt-BR pelo DescribeVoices. Não gera áudio. Não precisa salvar primeiro.
- **Testar voz** pede confirmação do consumo estimado e envia uma única solicitação de síntese. A voz inicial é Camila com o motor Padrão. Outras vozes e a frase ficam em Opções de voz. Se o MP3 chegar e for reproduzível, o app o toca automaticamente.
- As chaves digitadas não são salvas pelo teste. Quando a seção de edição está fechada, o teste usa as chaves salvas.
- **Gerenciar credenciais** reúne importação, cópia e exportação com as confirmações existentes e caminho para o IAM. **Consumo** mantém histórico e limites locais.

## Validação

Testes de política verificam o uso do formulário completo, credenciais salvas e ausência de mistura entre contas. A reprodução do MP3, o login de fato e o comportamento dos controles em aparelhos pequenos ainda devem ser testados no Android com uma conta AWS. A prévia da tela anexada à entrega é demonstrativa e não contém credenciais.

168 verificações estáticas passaram. O APK, os testes JVM via Gradle e o uso autenticado em Android não foram executados neste ambiente.
