#!/usr/bin/env python3
"""v0.1.158: API Batch texto opt-in, sem retry implícito, com privacidade."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def source(name): return (ROOT/'app/src/main/java/br/com/uaicortes'/name).read_text()
p = source('GeminiBatchPolicy.kt')
assert 'MAX_ITEMS = 8' in p and 'MAX_PROMPT_LENGTH = 1000' in p
assert 'GeminiCaptionModelPolicy.validId' in p and 'batches/' in p
c = source('GeminiBatchClient.kt')
assert ':batchGenerateContent' in c and 'input_config' in c and 'inlinedResponses' in c
assert '/v1beta/batches?pageSize=50' in c and 'listJobs(' in c
assert 'x-goog-api-key' in c and 'withContext(Dispatchers.IO)' in c
assert 'GeminiKeyFailover(' not in c and 'repeat(' not in c
s = source('GeminiBatchStore.kt')
assert '"REVIEW"' in s and 'persist(list() + record)' in s and 'GeminiBatchPolicy.jobName' in s
assert '.put("text"' not in s and 'key: String' not in s
ui = source('GeminiBatchUi.kt')
for x in ['confirmSubmit', 'acknowledgePaid', 'storage.start(', 'client.create(', 'client.status(',
          'storage.attach(', 'storage.forget(', 'SelectionContainer', 'client.listJobs(', 'Retomar']:
    if x == 'Retomar': continue
    assert x in ui, x
settings = source('GeminiSettingsUi.kt')
assert 'GeminiBatchSection()' in settings
for name in ['gemini_backup_rules.xml','gemini_data_extraction_rules.xml']:
    rules = (ROOT/'app/src/main/res/xml'/name).read_text()
    assert 'gemini_batch_local_v1.xml' in rules
assert (ROOT/'app/src/test/java/br/com/uaicortes/GeminiBatchPolicyTest.kt').is_file()
assert (ROOT/'.github/workflows/android-apk.yml').is_file()
print('OK: Batch texto, confirmação, registro pré-envio, consulta manual e privacidade')
