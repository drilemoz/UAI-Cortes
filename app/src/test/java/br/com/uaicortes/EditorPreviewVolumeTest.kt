package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class EditorPreviewVolumeTest {
    @Test fun gainAffectsOnlyPreview() {
        assertEquals(0.25f, EditorPreviewVolume.output(0.5f, 0.5f), 0.0001f)
        assertEquals(1f, EditorPreviewVolume.output(1f, 1f), 0.0001f)
        assertEquals(0f, EditorPreviewVolume.output(0.85f, 0f), 0.0001f)
    }
    @Test fun clampedAndNeverBoosts() {
        assertEquals(0f, EditorPreviewVolume.output(-1f, .5f), 0.0001f)
        assertEquals(1f, EditorPreviewVolume.output(2f, 2f), 0.0001f)
        assertEquals(40, EditorPreviewVolume.percent(.4f))
        assertEquals(100, EditorPreviewVolume.percent(5f))
    }
}
