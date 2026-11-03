package br.com.uaicortes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** Exige Android real/emulador: prova o caminho com JSONObject, filesDir e SHA-256. */
@RunWith(AndroidJUnit4::class)
class GeminiNarrationCheckpointAndroidTest {
    private fun wave(): ByteArray {
        val pcm = 48_000
        return ByteBuffer.allocate(44 + pcm).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcm); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(24_000); putInt(48_000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(pcm)
            repeat(pcm / 2) { putShort(0) }
        }.array()
    }

    @Test fun validBatchResumesButChangedWavIsRejected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val project = VideoProject(id = UUID.randomUUID().toString(),
            clips = listOf(Clip(uri = "file:///synthetic.mp4", title = "teste", sourceDurationMs = 8_000L)),
            subtitles = listOf(SubtitleCue(id = "cue-1", text = "Teste", startMs = 2_000L, endMs = 4_000L)))
        val segment = SubtitleNarrationTimeline.Segment("cue-1", "Texto da dublagem", 2_000L, 4_000L)
        val bytes = wave()
        val wav = GeminiAudioFiles.save(context, GeminiWave(bytes, 1_000L))
        val checkpoint = GeminiNarrationCheckpoint(context, project.id)
        val file = File(context.filesDir, "gemini_narration_sessions/projeto_${GeminiNarrationCheckpoint.hash(project.id.toByteArray())}.json")
        try {
            checkpoint.save(GeminiNarrationCheckpoint.Snapshot(
                durationMs = project.durationMs,
                text = segment.text,
                model = GeminiModels.LITE,
                voice = GeminiVoice("test-voice", "Voz de teste", ""),
                style = "Neutra", pace = 2, expressiveness = 2, pitch = 2,
                enabled = true, segments = listOf(segment),
                files = listOf(GeminiTimedFile(segment, wav, 1_000L,
                    "${GeminiModels.LITE}|test-voice|diretriz", sha256 = GeminiNarrationCheckpoint.hash(bytes))),
                starts = emptyMap(), goals = emptyMap()))
            val restored = GeminiNarrationCheckpoint(context, project.id).recover(project)
            assertEquals(1, restored.snapshot?.segments?.size)
            assertEquals(1, restored.snapshot?.files?.size)
            assertEquals("test-voice", restored.snapshot?.voice?.id)
            wav.writeText("arquivo corrompido")
            val damaged = GeminiNarrationCheckpoint(context, project.id).recover(project)
            assertEquals(1, damaged.snapshot?.segments?.size)
            assertEquals(0, damaged.snapshot?.files?.size)
            val changed = project.copy(subtitles = listOf(SubtitleCue(
                id = "cue-1", text = "Teste", startMs = 3_000L, endMs = 4_000L)))
            assertNull(GeminiNarrationCheckpoint(context, project.id).recover(changed).snapshot)
        } finally {
            wav.delete()
            file.delete()
        }
    }
}
