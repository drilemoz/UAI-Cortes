package br.com.uaicortes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayheadRuler298AndroidTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tapAndDragWorkOutsideTheGripAndBalanceScrubbingCallbacks() {
        val position = mutableLongStateOf(0L)
        var starts = 0
        var ends = 0
        compose.setContent {
            MaterialTheme {
                TimelinePlayheadRuler(200.dp, position.longValue, 10_000L, 10_000L,
                    { position.longValue = it }, { starts++ }, { ends++ })
            }
        }
        val ruler = compose.onNodeWithContentDescription("Cursor da linha do tempo")
        ruler.assertHeightIsEqualTo(48.dp)
        ruler.performTouchInput { click(Offset(center.x, 4f)) }
        compose.runOnIdle {
            assertTrue(position.longValue in 4_950L..5_050L)
            assertEquals(1, starts)
            assertEquals(starts, ends)
        }
        ruler.performTouchInput {
            swipe(Offset(width * .15f, center.y), Offset(width * .85f, center.y))
        }
        compose.runOnIdle {
            assertTrue(position.longValue in 8_400L..8_600L)
            assertEquals(2, starts)
            assertEquals(starts, ends)
        }
    }
}
