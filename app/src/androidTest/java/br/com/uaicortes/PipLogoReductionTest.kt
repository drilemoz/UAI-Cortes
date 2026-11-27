package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class PipLogoReductionTest {
    @Test fun thinTransparentStripesRetainCoverageAtSmallSizes() {
        val source = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        for (x in 0 until 64 step 4) {
            for (y in 0 until 32) source.setPixel(x, y, Color.BLUE)
        }
        val small = TimedImagePipOverlay.reduceForOutput(source, 8, 4)
        assertEquals(8, small.width)
        assertEquals(4, small.height)
        for (x in 0 until small.width) {
            val pixel = small.getPixel(x, 2)
            assertTrue(Color.alpha(pixel) in 60..68)
            assertTrue(Color.blue(pixel) >= 250)
        }
        assertSame(source, TimedImagePipOverlay.reduceForOutput(source, 64, 32))
    }
}
