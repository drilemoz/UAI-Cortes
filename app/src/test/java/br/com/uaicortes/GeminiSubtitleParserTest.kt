package br.com.uaicortes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GeminiSubtitleParserTest {
    @Test fun parsesDocumentedAnnotationsWithoutInventingTimings() {
        val response=JSONObject("""{"steps":[{"type":"model_output","content":[{"type":"text","text":"Olá mundo", "annotations":[{"type":"word_info","text":"Olá","start_offset":"0.100s","end_offset":"0.400s"},{"type":"word_info","text":"mundo","start_offset":"0.500s","end_offset":"0.800s"}]}]}]}""")
        val cues=GeminiSubtitleParser.parse(response,2000)
        assertEquals(1,cues.size)
        assertEquals("Olá mundo",cues[0].text)
        assertEquals(100L,cues[0].startMs)
        assertEquals(800L,cues[0].endMs)
        assertEquals(listOf(SubtitleWordTiming(100,400),SubtitleWordTiming(500,800)),cues[0].wordTimings)
    }

    @Test(expected=IllegalArgumentException::class)
    fun rejectsResponsesWithoutRealWords() {
        GeminiSubtitleParser.parse(JSONObject("""{"output_text":"Oi"}"""),2000)
    }

    @Test fun breaksLongSpeechIntoShortCues() {
        val annotations=(0..10).joinToString(",") { i ->
            """{"type":"word_info","text":"palavra$i","start_offset":"${"%.3f".format(java.util.Locale.ROOT,i*0.1)}s","end_offset":"${"%.3f".format(java.util.Locale.ROOT,i*0.1+0.05)}s"}"""
        }
        val data=JSONObject("""{"steps":[{"content":[{"annotations":[$annotations]}]}]}""")
        val cues=GeminiSubtitleParser.parse(data,15000)
        assertEquals(2,cues.size)
        assertEquals(7,cues[0].wordTimings.size)
        assertEquals(4,cues[1].wordTimings.size)
    }

    @Test fun diarizationSplitsAtSpeakerChangeWithoutChangingWordTimes() {
        val response=JSONObject("""{"steps":[{"content":[{"annotations":[
            {"type":"word_info","text":"Bom","start_offset":"0.100s","end_offset":"0.300s","speaker":"spk_1"},
            {"type":"word_info","text":"dia","start_offset":"0.310s","end_offset":"0.500s","speaker":"spk_1"},
            {"type":"word_info","text":"Olá","start_offset":"0.510s","end_offset":"0.850s","speaker":"spk_2"},
            {"type":"word_info","text":"voltei","start_offset":"0.900s","end_offset":"1.100s","speaker":"spk_1"}
        ]}]}]}""")
        val cues=GeminiSubtitleParser.parse(response,2000,withSpeakerLabels=true)
        assertEquals(listOf("Bom dia","Olá","voltei"),cues.map { it.text })
        assertEquals(listOf(100L,510L,900L),cues.map { it.startMs })
        assertEquals(listOf(500L,850L,1100L),cues.map { it.endMs })
        assertNotNull(cues[0].speakerId)
        assertEquals(cues[0].speakerId,cues[2].speakerId)
        assertNotEquals(cues[0].speakerId,cues[1].speakerId)
        assertEquals("Voz 1",GeminiDiarizationPolicy.label(cues[0].speakerId!!))
    }

    @Test fun disabledDiarizationDoesNotInferFromAnnotation() {
        val response=JSONObject("""{"steps":[{"content":[{"annotations":[
            {"type":"word_info","text":"Oi","start_offset":"0.1s","end_offset":"0.5s","speaker":"spk_1"}
        ]}]}]}""")
        assertNull(GeminiSubtitleParser.parse(response,1000,withSpeakerLabels=false).single().speakerId)
        assertNotNull(GeminiSubtitleParser.parse(response,1000,withSpeakerLabels=true).single().speakerId)
    }

    @Test fun rejectsUnknownSpeakerLabelsInsteadOfInventingIdentity() {
        val response=JSONObject("""{"steps":[{"content":[{"annotations":[
            {"type":"word_info","text":"Oi","start_offset":"0.1s","end_offset":"0.5s","speaker":"person_real_name"}
        ]}]}]}""")
        assertNull(GeminiSubtitleParser.parse(response,1000).single().speakerId)
    }

}
