#!/usr/bin/env python3
"""CI118: URI da voz não pode depender de android.net.Uri em testes JVM locais."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
core = (root/'app/src/main/java/br/com/uaicortes/GeminiTtsCore.kt').read_text()
assert 'fun uri(file: File): String = java.net.URI("file", "", file.absolutePath, null).toASCIIString()' in core
assert 'Uri.fromFile(' not in core
unit = (root/'app/src/test/java/br/com/uaicortes/VoiceIaPolicyTest.kt').read_text()
assert 'geminiFileUriCanBeBuiltWithoutAndroidAndEscapesUnicode' in unit
assert 'file:///tmp/voz%20com%20acento%20%C3%A1.wav' in unit
assert 'insertAndReplaceKeepOnlyOneVoiceWithoutTouchingMusic' in unit
print('OK: CI118: URI file:/// sem stub Android, com teste de unicode e inserção preservados')
