#!/usr/bin/env python3
"""v0.1.159: Batch integrado a Flow, sem aplicação automática nem duplicar cobranças."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
java=r/'app/src/main/java/br/com/uaicortes'
policy=(java/'FlowBatchPolicy.kt').read_text()
ui=(java/'FlowBatchUi.kt').read_text()
flow=(java/'FlowPromptScreen.kt').read_text()
client=(java/'GeminiBatchClient.kt').read_text()
store=(java/'GeminiBatchStore.kt').read_text()
for marker in ['FlowBatchPolicy.prepare', 'onBeforeSend()', 'storage.start(', 'client.createRequests(',
               'storage.accept(', 'client.status(', 'client.listJobs(', 'pendingApply',
               'FlowBatchPolicy.mayApply(', 'FlowBatchPolicy.resultFor(', 'Confirmar vínculo',
               'JOB_STATE_SUCCEEDED', 'onApply(index, answer)']:
    assert marker in ui, marker
assert 'FlowBatchSection(project.id, draft' in flow
assert 'scene' not in store or '.put("scene", scene.index)' in store
assert '.put("prompt"' not in store and '.put("text"' not in store and 'key: String' not in store
assert 'flowProjectId' in store and 'flowScenes' in store and 'persist(list() + record)' in store
assert 'flowProject' in store and 'flowRefs' in store
assert 'FlowBatchPolicy.instruction(scene, context, mode)' in (java/'GeminiVideoCaptionClient.kt').read_text()
assert 'MAX_FLOW_PROMPT_LENGTH' in client and 'generationConfig' in client
assert 'optJSONObject("output")' in client and 'optJSONArray("inlinedResponses")' in client
assert 'GeminiKeyFailover(' not in client and 'repeat(' not in client
assert 'fingerprint(current.normalized(), ref.index)' in policy
assert 'status.state != "JOB_STATE_SUCCEEDED"' in policy
assert 'result.length <= 1800' not in ui  # validation lives in policy
assert (r/'app/src/test/java/br/com/uaicortes/FlowBatchPolicyTest.kt').is_file()
for name in ('gemini_backup_rules.xml','gemini_data_extraction_rules.xml'):
    assert 'gemini_batch_local_v1.xml' in (r/'app/src/main/res/xml'/name).read_text()
assert (r/'.github/workflows/android-apk.yml').is_file()
print('OK: Flow Batch 159, metadados protegidos, revisão por cena e resposta inline REST')
