# Uai Cortes: Coqui XTTS-v2 + Chatterbox V3

Este é **um serviço Python opcional**, não um modelo embutido no APK. O celular
usa HTTPS para enviar **somente quando você tocar em Gerar** o texto da fala e
o WAV de referência do personagem selecionado. A resposta é WAV e o aplicativo
insere a faixa privada na timeline, sem chamar Gemini.

## Preparação em PC Linux / VPS com RAM suficiente

Use Python 3.10 ou 3.11 e um ambiente isolado. Os motores têm requisitos de
PyTorch/torchaudio e versões CUDA que podem variar por GPU e plataforma;
instale primeiro PyTorch/torchaudio compatíveis conforme o site do PyTorch,
depois `coqui-tts` e `chatterbox-tts` conforme os projetos oficiais.
Não há pesos de modelos neste ZIP. O primeiro uso pode baixar arquivos grandes.

```bash
python3.11 -m venv .venv
source .venv/bin/activate
python -m pip install -U pip
# Instale torch + torchaudio compatíveis antes dos pacotes abaixo.
pip install coqui-tts chatterbox-tts
export UAI_VOICE_TOKEN="$(python -c 'import secrets; print(secrets.token_urlsafe(36))')"
python tools/voice_bridge/server.py --host 127.0.0.1 --port 8765
```

As dependências dos dois motores **podem entrar em conflito** no mesmo ambiente.
Nesse caso, use dois ambientes/serviços separados e configure o proxy para o
motor desejado. Este exemplo compartilha um processo e precisa ser testado no
hardware alvo; não considere o comando acima garantia de instalação funcional.

## HTTPS obrigatório no Android

No computador/servidor, publique `127.0.0.1:8765` somente atrás de proxy
HTTPS com certificado TLS confiável (Caddy, Nginx, túnel confiável). O aplicativo
**recusa HTTP, mesmo na rede Wi-Fi**, para não expor tokens e áudios. Não
desative a verificação de certificados. Não exponha diretamente a porta 8765.

Exemplo Caddyfile (substitua pelo SEU domínio apontado ao servidor):

```
voz.seudominio.example {
  reverse_proxy 127.0.0.1:8765
  request_body {
    max_size 14MB
  }
}
```

O token configurado em `UAI_VOICE_TOKEN` deve ser copiado para o campo no app.
Ele fica apenas em RAM no formulário; somente o endereço HTTPS é lembrado.
Na tela **Vozes IA > Clonar voz**: importe WAV limpo de 3 a 30 segundos, selecione
Coqui ou Chatterbox, fixe ao personagem, configure servidor/token e confirme
**Gerar e inserir esta fala**. Não há síntese em lote automática.

## Licenças, privacidade e limites

- Chatterbox: repositório MIT, projeto original arquivado em 2026. Modelo geral
  oferece idioma `pt`, mas não garante sotaque brasileiro. Não instalar pacotes
  com versões aleatórias em servidor exposto.
- Coqui XTTS-v2: a licença do **modelo** tem restrições de uso comercial.
  Confirme os termos antes de monetizar vídeos com sua saída.
- Utilize apenas vozes suas ou com autorização do titular. O serviço não faz
  reconhecimento de identidade, separação de locutores, limpeza de música ou
  sincronização labial.
- Para segurança/recursos, limite atual: WAV referência 10 MiB, 3 a 30 s;
  texto 800 caracteres; WAV gerado 24 MiB. Uma geração por vez por servidor.
- Em erro/timeout, verifique o serviço; **não existe repetição automática**.
- Os arquivos permanecem privados no Android. Excluir um perfil não apaga
  faixas já inseridas, mas impede novas gerações dependentes da referência.

Referências: https://github.com/idiap/coqui-ai-TTS,
https://coqui-tts.readthedocs.io/en/latest/cloning.html,
https://github.com/resemble-ai/chatterbox
