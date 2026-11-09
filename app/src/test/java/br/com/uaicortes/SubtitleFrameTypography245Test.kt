package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleFrameTypography245Test {
    @Test fun previewPixelsAndExportFrameShareScale() {
        for (width in listOf(280,360,720,1080)) {
            val nominal=SubtitleFrameTypography.fontPixelsForFrame(22,width)
            val preview=SubtitleFrameTypography.previewSizeSp(22,width.toFloat(),1f)
            assertEquals(nominal,preview,.001f)
        }
    }
    @Test fun textAreaIsAlwaysNinetyPercent() {
        assertEquals(648,SubtitleFrameTypography.allowedTextWidth(720))
        assertEquals(972,SubtitleFrameTypography.allowedTextWidth(1080))
    }
}
