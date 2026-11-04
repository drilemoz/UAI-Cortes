#!/usr/bin/env python3
"""CI105: Kotlin visibility of project Flow draft. Static source contract.

Does not replace Android Gradle compilation. The error surfaced in both
compileDebugKotlin and compileReleaseKotlin during workflow 105.
"""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
models = (root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
flow = (root/'app/src/main/java/br/com/uaicortes/FlowPrompt.kt').read_text()
assert re.search(r'(?m)^data class VideoProject\s*\(', models), 'VideoProject deve continuar pública'
assert re.search(r'(?m)^data class FlowPromptDraft\s*\(', flow), 'FlowPromptDraft pública, sem internal/private'
assert not re.search(r'(?m)^(?:internal|private)\s+data class FlowPromptDraft', flow)
assert 'val flowPromptDraft: FlowPromptDraft? = null' in models
assert '.put("flowPromptDraft", flowPromptDraft?.json())' in models
assert 'FlowPromptDraft.read(j.optJSONObject("flowPromptDraft"))' in models
assert '.put("schemaVersion", 4)' in models
assert 'internal object FlowPromptPolicy' in flow
print('OK: CI105 — VideoProject e FlowPromptDraft têm visibilidade pública compatível, JSON schema 4 preservado')
