package br.com.uaicortes

import br.com.uaicortes.EditorDiagnosticPolicy.MediaStage
import br.com.uaicortes.EditorDiagnosticPolicy.Result
import br.com.uaicortes.EditorDiagnosticPolicy.Target
import br.com.uaicortes.EditorDiagnosticPolicy.TransitionStage
import org.junit.Assert.*
import org.junit.Test

class EditorDiagnosticsAndPlayheadTest {
    @Test fun mapsRulerAcrossZoomAndLimitsTheTemporaryExtension() {
        assertEquals(2_500L, TimelinePlayheadPosition.fromX(250f, 1_000, 10_000, 10_000))
        assertEquals(8_000L, TimelinePlayheadPosition.fromX(400f, 500, 10_000, 8_000))
        assertEquals(8_000L, TimelinePlayheadPosition.fromX(499f, 500, 10_000, 8_000))
        assertEquals(10_000L, TimelinePlayheadPosition.fromX(1_005f, 1_000, 10_000, 10_000))
        assertEquals(0L, TimelinePlayheadPosition.fromX(-5f, 500, 10_000, 8_000))
        assertEquals(0L, TimelinePlayheadPosition.fromX(Float.NaN, 0, 10_000, 8_000))
    }

    @Test fun mediaEventsDistinguishTargetsAndNumericCodecWithoutFreeText() {
        assertEquals("target=main_image stage=import_copy result=failed",
            EditorDiagnosticPolicy.media(Target.MAIN_IMAGE, MediaStage.IMPORT_COPY, Result.FAILED))
        assertEquals("target=pip_image stage=preview_decode result=invalid_image",
            EditorDiagnosticPolicy.media(Target.pip(true), MediaStage.PREVIEW_DECODE, Result.INVALID_IMAGE))
        assertEquals("target=pip_video stage=preview_playback result=failed media3=4003",
            EditorDiagnosticPolicy.media(Target.pip(false), MediaStage.PREVIEW_PLAYBACK, Result.FAILED, 4003))
    }

    @Test fun transitionEventsUseOrdinalsAndDoNotEchoArbitraryStrings() {
        assertEquals("mode=preview stage=frame_prep_failed junction=3 effect=dissolve",
            EditorDiagnosticPolicy.transition(true, TransitionStage.FRAME_PREP_FAILED, 2, JunctionTransitionPlan.DISSOLVE))
        assertEquals("mode=preview stage=play_failed junction=1 effect=unknown media3=4003",
            EditorDiagnosticPolicy.transition(true, TransitionStage.PLAY_FAILED, 0, "file:///meuVideo.mp4", 4003))
        assertEquals("mode=export stage=render_failed media3=1000",
            EditorDiagnosticPolicy.transition(false, TransitionStage.RENDER_FAILED, playerCode = 1000))
    }
}
