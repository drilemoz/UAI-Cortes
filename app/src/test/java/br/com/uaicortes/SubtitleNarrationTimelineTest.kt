package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleNarrationTimelineTest {
    private fun s(id: String, start: Long, end: Long, text: String = "Olá") =
        SubtitleNarrationTimeline.Segment(id, text, start, end)

    @Test fun lateSpeechKeepsItsOwnStart() {
        val lines = listOf(s("late", 16000, 19000))
        assertNull(SubtitleNarrationTimeline.validate(lines, 20000))
        val record = SubtitleNarrationTimeline.inspect(listOf(SubtitleNarrationTimeline.Recorded(lines[0], 2800)), 20000)
        assertEquals(16000L, record[0].segment.startMs)
        assertTrue(record[0].issues.isEmpty())
    }
    @Test fun overflowIsBlockedRatherThanTruncated() {
        val line = s("late", 18000, 19500)
        val record = SubtitleNarrationTimeline.inspect(listOf(SubtitleNarrationTimeline.Recorded(line, 3000)), 20000)
        assertTrue(record.single().blocking)
        assertEquals(3000L, record.single().audioDurationMs)
    }
    @Test fun subtitleOverrunIsWarningWithoutEndTruncation() {
        val line = s("a", 1000, 2000)
        val report = SubtitleNarrationTimeline.inspect(listOf(SubtitleNarrationTimeline.Recorded(line, 1200)), 5000)
        assertTrue(report.single().needsApproval)
        assertFalse(report.single().blocking)
    }
    @Test fun nextSpeechOverlapIsDetected() {
        val a = s("a", 1000, 2000)
        val b = s("b", 2400, 4000)
        val report = SubtitleNarrationTimeline.inspect(listOf(
            SubtitleNarrationTimeline.Recorded(a, 1600), SubtitleNarrationTimeline.Recorded(b, 1200)), 5000)
        assertTrue(SubtitleNarrationTimeline.Issue.NEXT_SPEECH_OVERLAP in report.first().issues)
    }
    @Test fun invalidOrOversizedBatchesAreRejectedBeforeNetwork() {
        val lines = (0 until 13).map { s("$it", it * 1000L, it * 1000L + 500) }
        assertNotNull(SubtitleNarrationTimeline.validate(lines, 16000))
        assertNotNull(SubtitleNarrationTimeline.validate(listOf(s("a", 19900, 21000)), 20000))
        assertNotNull(SubtitleNarrationTimeline.validate(listOf(s("a", 0, 1000), s("a", 2000, 3000)), 10000))
    }
}
