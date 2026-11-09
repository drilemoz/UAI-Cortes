#!/usr/bin/env python3
"""v246: diagnóstico real, importação segura e acesso ao reparo."""
from pathlib import Path
r = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
imp = (r/'StaticImageClip.kt').read_text()
policy = (r/'ImageImportSafety.kt').read_text()
ui = (r/'MainImageInsertionUi.kt').read_text()
repair = (r/'ProjectMediaRepairScreen.kt').read_text()
availability = (r/'ProjectMediaAvailability.kt').read_text()
activity = (r/'MainActivity.kt').read_text()
verified = (r/'MediaRelinkVerification.kt').read_text()
diagnostic = (r/'DiagnosticRecorder.kt').read_text()
assert 'ImageImportSafety.recognizedMime(reportedMime, options.outMimeType)' in imp
assert 'runCatching { context.contentResolver.getType(uri) }' in imp
assert 'runCatching {\n            context.contentResolver.query(' in imp
assert imp.count('currentCoroutineContext().ensureActive()') >= 2
assert 'partial.delete(); output.delete(); throw failure' in imp
assert '"image/jpg", "image/pjpeg" -> "image/jpeg"' in policy
assert 'actual != null -> actual.takeIf { it in accepted }' in policy
assert 'ImageImportSafety.userMessage(error)' in ui
assert 'ImageImportSafety.diagnosticKind(error)' in ui
assert 'openInputStream(uri)' in availability
assert 'catch (cancelled: CancellationException)' in ui
assert 'catch (cancelled: CancellationException)' in repair
assert 'provisionalCopy?.let' in repair
assert 'ImageClipImport.discardUnused(' in repair
assert 'if (target.mimePrefix != "image/")' in verified
assert 'onRepair: () -> Unit' in availability
assert 'Text("Reparar mídias")' in availability
assert 'onRepair = { navTo("media_repair") }' in activity
assert 'DiagnosticRecorder.mediaAvailability(missing)' in availability
assert 'fun mediaAvailability(missing: Int)' in diagnostic
assert 'state=inaccessible count=' in diagnostic
print('OK: v246 robustez, cancelamento, reparo e diagnóstico')
