package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ImageAnimationDraftRulesTest {
    @Test fun noChanges() {
        val options=ImageAnimationOptions()
        assertFalse(ImageAnimationDraftRules.changed(listOf("content://photo/1"),options,
            listOf("content://photo/1"), options))
    }
    @Test fun orderChangeIsDetected() {
        val options=ImageAnimationOptions()
        assertTrue(ImageAnimationDraftRules.changed(listOf("1","2"),options,
            listOf("2","1"),options))
    }
    @Test fun settingsChangeIsDetected() {
        assertTrue(ImageAnimationDraftRules.changed(listOf("1"),ImageAnimationOptions(),
            listOf("1"),ImageAnimationOptions(quality=1080)))
    }
    @Test fun removalIsDetected() {
        assertTrue(ImageAnimationDraftRules.changed(listOf("1"),ImageAnimationOptions(),
            emptyList(),ImageAnimationOptions()))
    }
}
