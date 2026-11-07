#!/usr/bin/env python3
"""v216: cancelamento durante cópia, seleção preservada e responsividade.
Contrato de fonte; não substitui instrumentação Android.
"""
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
ui=(p/'QuickVideoToolsScreen.kt').read_text()
engine=(p/'QuickVideoSplitEngine.kt').read_text()
saver=(p/'GalleryVideoSaver.kt').read_text()
for expected in (
    'lastSecondsDuration by rememberSaveable', 'lastMinutesDuration by rememberSaveable',
    'durationGroup = DurationGroup.SECONDS; partDuration = lastSecondsDuration',
    'durationGroup = DurationGroup.MINUTES; partDuration = lastMinutesDuration',
    'if (durationGroup == DurationGroup.SECONDS) lastSecondsDuration = partDuration',
    'else lastMinutesDuration = partDuration',
    'val stackCustom = maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f',
    'QuickCustomTimeField("Horas"', 'QuickCustomTimeField("Min"',
    'QuickCustomTimeField("Seg"',
): assert expected in ui, expected
assert ui.count('lastSecondsDuration = partDuration') == 2
assert 'private fun QuickCustomTimeField(' in ui
assert '@Volatile private var activeGeneration = 0' in engine
assert 'subfolder = "Divididos", shouldContinue = { isCurrent() })' in engine
assert engine.index('if (!isCurrent()) return@post') < engine.index('saving = false', engine.index('main.post {', engine.index('GalleryVideoSaver.save(')))
assert 'shouldContinue: () -> Boolean = { true }' in saver
assert 'while (true)' in saver and 'input.read(buffer)' in saver
assert saver.count('if (!shouldContinue()) throw java.util.concurrent.CancellationException(') == 2
assert saver.index('if (!shouldContinue())', saver.index('while (true)')) < saver.index('output.write(buffer, 0, size)')
assert saver.rindex('if (!shouldContinue())') < saver.index('resolver.update(target, ContentValues()')
assert 'resolver.delete(target, null, null)' in saver
print('OK: v216 cancelamento cooperativo, callback antigo isolado e UI responsiva')
