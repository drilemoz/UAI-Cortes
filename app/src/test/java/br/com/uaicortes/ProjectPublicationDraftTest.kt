package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ProjectPublicationDraftTest {
    @Test fun projectRoundtripKeepsDraftWithoutChangingSchema() {
        val draft = VideoPublicationDraft("Um momento inesperado", "#Humor #Cortes #Uai #Video #Curiosidade")
        val project = VideoProject(id = "proj-42", title = "Meu corte", publicationDraft = draft)
        val json = project.json()
        assertEquals(4, json.getInt("schemaVersion"))
        assertEquals(draft, VideoProject.read(JSONObject(json.toString())).publicationDraft)
        assertEquals("proj-42", VideoProject.read(json).id)
    }

    @Test fun legacyProjectWithoutDraftLoadsNormally() {
        val json = VideoProject(title = "Antigo").json()
        json.remove("publicationDraft")
        assertNull(VideoProject.read(json).publicationDraft)
    }

    @Test fun incompleteDraftIsKeptButCannotBeCopied() {
        val project = VideoProject(publicationDraft = VideoPublicationDraft("Texto pendente", "#Uma"))
        val restored = VideoProject.read(project.json()).publicationDraft!!
        assertEquals("Texto pendente", restored.description)
        assertFalse(VideoCaptionPolicy.isValid(restored.description, restored.hashtags))
    }

    @Test fun backupCodecAlsoPreservesPublicationDraft() {
        val project = VideoProject(publicationDraft = VideoPublicationDraft(
            "Um dia de edição", "#Video #Uai #Cortes #Teste #Criacao"))
        val encoded = ProjectBackupCodec.encode(project, 42L)
        val decoded = ProjectBackupCodec.decode(encoded).first
        assertEquals(project.publicationDraft, decoded.publicationDraft)
    }

    @Test fun importedOverlyLongFieldsAreBounded() {
        val json = VideoProject().json()
        json.put("publicationDraft", JSONObject()
            .put("description", "A".repeat(2000)).put("hashtags", "#".repeat(2000)))
        val result = VideoProject.read(json).publicationDraft!!
        assertEquals(VideoPublicationDraft.MAX_FIELD_LENGTH, result.description.length)
        assertEquals(VideoPublicationDraft.MAX_FIELD_LENGTH, result.hashtags.length)
    }
}
