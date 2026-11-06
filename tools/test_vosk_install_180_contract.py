#!/usr/bin/env python3
"""Contratos estruturais do instalador Vosk 0.1.180 (não substitui teste Android)."""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
engine = (r/'app/src/main/java/br/com/uaicortes/AutoSubtitleEngine.kt').read_text()
installer = (r/'app/src/main/java/br/com/uaicortes/VoskArchiveInstaller.kt').read_text()
feedback = (r/'app/src/main/java/br/com/uaicortes/VoskInstallFeedback.kt').read_text()
ui = (r/'app/src/main/java/br/com/uaicortes/SpeechModelSettingsScreen.kt').read_text()
unit = (r/'app/src/test/java/br/com/uaicortes/VoskArchiveInstallerTest.kt').read_text()
assert 'installationMutex.withLock' in engine
assert 'VoskArchiveInstaller.extract' in engine and 'VoskArchiveInstaller.installValidated' in engine
assert 'currentCoroutineContext()' in engine and 'context.ensureActive()' in engine
assert 'zip.delete(); unpack.deleteRecursively()' in engine
for v in ('ZipFile(zipFile)', 'CRC32()', 'item.crc', 'canonicalFile', 'MAX_ENTRIES',
          'DUPLICATE_ENTRY', 'UNSAFE_PATH', 'TOO_LARGE', 'installValidated',
          'backup.renameTo(target)', 'VoskModelLayout.detect(target) != null'):
    assert v in installer, v
for v in ('WRONG_MODEL', 'INVALID_ZIP', 'CHECKSUM', 'INSTALL_FAILED', 'network_timeout',
          'file_permission'):
    assert v in feedback or v in installer, v
assert 'DiagnosticRecorder.action("speech_model", "vosk_zip_" + VoskInstallFeedback.code(e))' in ui
assert 'DiagnosticRecorder.action("speech_model", "vosk_download_" + VoskInstallFeedback.code(e))' in ui
assert 'failure = VoskInstallFeedback.message(e)' in ui
assert 'e.message?.take(180)' not in ui
for v in ('officialPortugueseZipIsAccepted', 'zipWithExtraDirectoryIsAccepted',
          'rejectsOtherModelAndMissingFiles', 'rejectsUnsafeAndOversizedZip',
          'restoresValidPreviousModelWhenInstallWasInterrupted'):
    assert v in unit, v
assert 'schemaVersion' in (r/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
print('OK: Vosk ZIP com CRC, rollback, limite, segurança, código de diagnóstico e testes')
