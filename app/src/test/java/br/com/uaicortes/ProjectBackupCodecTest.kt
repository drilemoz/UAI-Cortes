package br.com.uaicortes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProjectBackupCodecTest {
    @Test fun roundTripKeepsProjectDataAndSchemaFour() {
        val project = VideoProject(id="test-id", title="Teste", subtitles=listOf(
            SubtitleCue(id="s1", text="Olá", startMs=0, endMs=1200)))
        val encoded = ProjectBackupCodec.encode(project, 1234)
        val (decoded, time) = ProjectBackupCodec.decode(encoded)
        assertEquals(1234, time)
        assertEquals(project.id, decoded.id)
        assertEquals("Olá", decoded.subtitles.single().text)
        assertEquals(4, JSONObject(JSONObject(encoded).getString("payload")).getInt("schemaVersion"))
    }
    @Test fun tamperedPayloadCannotBeRestored() {
        val backup = JSONObject(ProjectBackupCodec.encode(VideoProject(id="valid"), 42))
        backup.put("payload", backup.getString("payload").replace("valid", "other"))
        try {
            ProjectBackupCodec.decode(backup.toString())
            fail("Modified payload must not pass digest check")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun unknownFormatIsRejected() {
        val backup = JSONObject(ProjectBackupCodec.encode(VideoProject(id="valid"), 42))
        backup.put("format", "unknown")
        try { ProjectBackupCodec.decode(backup.toString()); fail("Must reject version") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun oversizedInputIsRejected() {
        try { ProjectBackupCodec.decode("x".repeat(ProjectBackupCodec.MAX_BYTES + 1)); fail("size limit") }
        catch (_: IllegalArgumentException) { }
    }
}
