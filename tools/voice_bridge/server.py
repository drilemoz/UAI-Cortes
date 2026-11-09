#!/usr/bin/env python3
"""Backend TTS opcional do Uai Cortes. Python no PC/VPS, NÃO dentro do APK.

Instale em ambiente de confiança com Python 3.10/3.11, pytorch/torchaudio
compatíveis e os pacotes coqui-tts + chatterbox-tts conforme instruções oficiais.
Coloque por trás de HTTPS (ex.: Caddy) e use token de alta entropia.
"""
from __future__ import annotations

import base64
import binascii
import hmac
import io
import json
import os
from pathlib import Path
import tempfile
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MAX_BODY = 14_000_000
MAX_REFERENCE = 10 * 1024 * 1024
MAX_OUTPUT = 24 * 1024 * 1024
MOTORS = ("coqui_xtts_v2", "chatterbox_v3")
ENGINE_LOCK = threading.Lock()
MODELS = {}


def validate(data: dict):
    if not isinstance(data, dict):
        raise ValueError("Corpo JSON inválido")
    if data.get("engine") not in MOTORS or data.get("language") != "pt":
        raise ValueError("Motor ou idioma não suportado")
    text = data.get("text")
    if not isinstance(text, str) or not 1 <= len(text.strip()) <= 800:
        raise ValueError("Texto deve ter 1 a 800 caracteres")
    voice_id = data.get("voice_id")
    if not isinstance(voice_id, str) or len(voice_id) != 36 or any(
            char not in "0123456789abcdef-" for char in voice_id):
        raise ValueError("Identificador de voz inválido")
    encoded = data.get("reference_wav_b64")
    if not isinstance(encoded, str) or len(encoded) > 13_981_016:
        raise ValueError("Referência excessiva")
    try:
        wav = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise ValueError("Referência base64 inválida") from exc
    if not 128 <= len(wav) <= MAX_REFERENCE or wav[:4] != b"RIFF" or wav[8:12] != b"WAVE":
        raise ValueError("Referência WAV inválida")
    return data["engine"], text, wav


def get_model(engine: str):
    if engine not in MODELS:
        import torch
        device = "cuda" if torch.cuda.is_available() else "cpu"
        if engine == "coqui_xtts_v2":
            from TTS.api import TTS
            MODELS[engine] = TTS("tts_models/multilingual/multi-dataset/xtts_v2").to(device)
        else:
            from chatterbox.mtl_tts import ChatterboxMultilingualTTS
            MODELS[engine] = ChatterboxMultilingualTTS.from_pretrained(device=device, t3_model="v3")
    return MODELS[engine]


def generate(engine: str, text: str, reference: bytes) -> bytes:
    """Processa uma solicitação por vez para proteger RAM/VRAM e preservar a voz."""
    with tempfile.TemporaryDirectory(prefix="uai_voice_") as temp:
        ref_path = Path(temp) / "reference.wav"
        out_path = Path(temp) / "result.wav"
        ref_path.write_bytes(reference)
        model = get_model(engine)
        if engine == "coqui_xtts_v2":
            model.tts_to_file(text=text, speaker_wav=str(ref_path),
                              language="pt", file_path=str(out_path))
        else:
            import torchaudio
            wave = model.generate(text, language_id="pt", audio_prompt_path=str(ref_path))
            torchaudio.save(str(out_path), wave.cpu(), model.sr)
        result = out_path.read_bytes()
        if len(result) < 128 or len(result) > MAX_OUTPUT or result[:4] != b"RIFF" or result[8:12] != b"WAVE":
            raise RuntimeError("Modelo não gerou WAV utilizável")
        return result

class Handler(BaseHTTPRequestHandler):
    server_version = "UaiVoiceBridge/1"
    def log_message(self, format, *args):
        # Não registrar textos, referências, caminhos privados ou tokens.
        pass

    def response(self, status, data: bytes, content_type="application/json"):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        token = os.environ.get("UAI_VOICE_TOKEN", "")
        provided = self.headers.get("Authorization", "")
        if len(token) < 20 or not hmac.compare_digest(provided, "Bearer " + token):
            return self.response(401, b'{"error":"unauthorized"}')
        if self.path != "/api/v1/tts":
            return self.response(404, b'{"error":"not_found"}')
        if self.headers.get("Content-Type", "").split(";", 1)[0] != "application/json":
            return self.response(415, b'{"error":"unsupported_content_type"}')
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            length = 0
        if not 2 <= length <= MAX_BODY:
            return self.response(413, b'{"error":"size_limit"}')
        try:
            data = json.loads(self.rfile.read(length))
            engine, text, reference = validate(data)
        except (ValueError, UnicodeError, json.JSONDecodeError):
            return self.response(400, b'{"error":"invalid_request"}')
        if not ENGINE_LOCK.acquire(blocking=False):
            return self.response(429, b'{"error":"busy"}')
        try:
            result = generate(engine, text, reference)
        except Exception:
            # As bibliotecas podem revelar caminhos e dados internos em exceções.
            return self.response(500, b'{"error":"synthesis_failed"}')
        finally:
            ENGINE_LOCK.release()
        return self.response(200, result, "audio/wav")


def main():
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    if len(os.environ.get("UAI_VOICE_TOKEN", "")) < 20:
        parser.error("Configure UAI_VOICE_TOKEN com ao menos 20 caracteres")
    print(f"Uai Voice Bridge escutando em {args.host}:{args.port}. Exponha apenas via HTTPS autenticado.")
    ThreadingHTTPServer((args.host, args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
