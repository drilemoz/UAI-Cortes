#!/usr/bin/env python3
"""CI126: resolver teste Kotlin obsoleto sobre 480p sem enfraquecer validação.

Esta verificação estática complementa os testes JVM/Gradle, não os substitui.
"""
from pathlib import Path
import re
r=Path(__file__).resolve().parents[1]
main=(r/'app/src/main/java/br/com/uaicortes/ExportTaskCoordinator.kt').read_text(encoding='utf8')
test=(r/'app/src/test/java/br/com/uaicortes/ExportTaskCoordinatorTest.kt').read_text(encoding='utf8')
screen=(r/'app/src/main/java/br/com/uaicortes/ExportScreen.kt').read_text(encoding='utf8')
allowed={int(v.strip()) for v in re.search(r'require\(quality in listOf\(([^)]+)\)',main).group(1).split(',')}
accepted={int(v.strip()) for v in re.search(r'fun everyAdvertisedResolutionIsAccepted\(\).*?listOf\(([^)]+)\)',test,re.S).group(1).split(',')}
invalid={int(v.strip()) for v in re.search(r'fun invalidResolutionOrFileNameAreRejected\(\).*?listOf\(([^)]+)\)',test,re.S).group(1).split(',')}
ui={int(v.strip()) for v in re.search(r'listOf\((\d+(?:,\d+)+)\)\.forEach',screen).group(1).split(',')}
assert allowed == accepted == ui == {360,480,720,1080}, (allowed,accepted,ui)
assert invalid and allowed.isdisjoint(invalid), (allowed,invalid)
assert 240 in invalid and 2160 in invalid
assert 'for (resolution in listOf(480, 2160))' not in test
print('OK: CI126 — resolução 480p aceita pela tela, coordenador e JUnit; valores fora da lista rejeitados')
