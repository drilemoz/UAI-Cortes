#!/usr/bin/env python3
"""Contrato do pedido e do tratamento local de erros. Não chama AWS."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"
api = (src / "PollyApi.kt").read_text()
err = (src / "PollyResponseError.kt").read_text()
assert "IncludeAdditionalLanguageCodes" not in api
assert 'if (token.isNullOrEmpty()) "LanguageCode=pt-BR"' in api
assert 'PollySigV4.encode(token)' in api
assert 'root.opt("NextToken") as? String' in api
assert 'conn.getHeaderField("x-amzn-errortype")' in api
assert 'readLimited(it, 12_288)' in api
assert 'throw PollyResponseError.exception(action, status, type, body)' in api
assert 'takeIf { it in known }' in err
assert 'optString("message")' not in err and 'optString("Message")' not in err
assert 'não confirma problema na chave' in err
assert (root / "app/src/test/java/br/com/uaicortes/PollyResponseErrorTest.kt").is_file()
print("OK: consulta pt-BR, paginação e diagnóstico sem resposta bruta")
