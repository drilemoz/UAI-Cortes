package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TranscriptionStageProgressTest {
    @Test fun progressUsesDecodedFractionsNotElapsedTime() {
        assertEquals(0,TranscriptionStageProgress.decoding(0,1,0f))
        assertEquals(49,TranscriptionStageProgress.decoding(0,1,.5f))
        assertEquals(98,TranscriptionStageProgress.decoding(0,1,1f))
        assertEquals(49,TranscriptionStageProgress.decoding(1,2,0f))
        assertEquals(73,TranscriptionStageProgress.decoding(1,2,.5f))
    }
    @Test fun malformedInputsDoNotProduceFalseCompletion() {
        assertEquals(0,TranscriptionStageProgress.decoding(0,0,1f))
        assertEquals(0,TranscriptionStageProgress.decoding(-1,2,Float.NaN))
        assertEquals(0,TranscriptionStageProgress.decoding(0,1,Float.POSITIVE_INFINITY))
        assertEquals(98,TranscriptionStageProgress.decoding(100,2,1f))
    }
}
