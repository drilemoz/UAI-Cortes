#!/usr/bin/env python3
"""Regressão estática do fim de projeto e do segundo plano no player de edição."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
editor = (root/'app/src/main/java/br/com/uaicortes/EditorScreen.kt').read_text()
assert 'repeatMode = Player.REPEAT_MODE_OFF' in editor
assert 'Player.REPEAT_MODE_ALL' not in editor
for expected in [
    'val lifecycleOwner = LocalLifecycleOwner.current',
    'DisposableEffect(player, lifecycleOwner)',
    'LifecycleEventObserver',
    'Lifecycle.Event.ON_PAUSE',
    'Lifecycle.Event.ON_STOP',
    'player.pause()',
    'lifecycleOwner.lifecycle.addObserver(observer)',
    'lifecycleOwner.lifecycle.removeObserver(observer)',
    'if (player.playbackState == Player.STATE_ENDED) seekGlobal(0L)',
    'lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)',
    'player.release()'
]:
    assert expected in editor, expected
assert re.search(r'if \(event == Lifecycle.Event.ON_PAUSE \|\| event == Lifecycle.Event.ON_STOP\) \{\s*player.pause\(\)',editor)
assert re.search(r'if \(player.playbackState == Player.STATE_ENDED\) seekGlobal\(0L\)\s*player.play\(\)',editor)
print('OK: v0.1.237 editor encerra em fim, pausa em background e reinicia sob comando')
