#!/usr/bin/env python3
"""Gera notas concisas para GitHub Releases sem apagar o histórico completo.

A API do GitHub rejeita o corpo com mais de 125 mil caracteres.
Este script publica apenas as notas da versão atual, com limite conservador.
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
# Mantemos margem suficiente abaixo do limite de 125000 caracteres da API.
MAX_PUBLICATION_BYTES = 80_000
HEADING = re.compile(r"(?m)^# Uai Cortes[^\n]*versão\s+(\d+\.\d+\.\d+)[^\n]*$")
REPOSITORY = re.compile(r"^[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}$")


def render(raw: str, version: str, repository: str = "") -> str:
    headers = list(HEADING.finditer(raw))
    if not headers or headers[0].group(1) != version:
        raise ValueError(f"RELEASE_NOTES.md não começa pela versão atual: {version}")
    end = headers[1].start() if len(headers) > 1 else len(raw)
    latest = raw[headers[0].start():end].strip()
    if not latest or len(latest.splitlines()) < 2:
        raise ValueError("Notas da versão atual não possuem descrição")
    footer = (
        "\n\n---\n"
        "Histórico integral preservado nos arquivos `RELEASE_NOTES.md` e `CHANGELOG.md` do repositório."
    )
    if repository and REPOSITORY.fullmatch(repository):
        footer += (
            f"\n[Histórico completo](https://github.com/{repository}/blob/main/RELEASE_NOTES.md)"
            f" · [Alterações](https://github.com/{repository}/blob/main/CHANGELOG.md)"
        )
    # Não cortar no meio de um caractere UTF-8 ou publicar corpo maior que o limite.
    available_bytes = MAX_PUBLICATION_BYTES - len(footer.encode('utf-8')) - 200
    if available_bytes <= 0:
        raise ValueError("Rodapé das notas maior que o limite seguro")
    encoded = latest.encode('utf-8')
    if len(encoded) > available_bytes:
        partial = encoded[:available_bytes].decode('utf-8', errors='ignore')
        newline = partial.rfind('\n')
        if newline > 100:
            partial = partial[:newline]
        latest = partial.rstrip() + "\n\n*(Notas encurtadas para publicação; consulte o histórico completo.)*"
    result = latest + footer + "\n"
    if len(result.encode('utf-8')) > MAX_PUBLICATION_BYTES:
        raise ValueError("Notas de publicação excedem o limite de segurança")
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', default='dist/RELEASE_NOTES_GITHUB.md')
    args = parser.parse_args()
    version = (ROOT / 'VERSION').read_text(encoding='utf-8').strip()
    source = (ROOT / 'RELEASE_NOTES.md').read_text(encoding='utf-8')
    notes = render(source, version, os.environ.get('GITHUB_REPOSITORY', ''))
    output = Path(args.output)
    if not output.is_absolute():
        output = ROOT / output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(notes, encoding='utf-8')
    print(f"OK: Release v{version}: {len(notes.encode('utf-8'))} bytes de notas publicáveis em {output}")


if __name__ == '__main__':
    main()
