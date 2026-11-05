package br.com.uaicortes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Necessita emulador/aparelho: cobre JSON e AtomicFile com texto real em arquivo privado. */
@RunWith(AndroidJUnit4::class)
class GeminiTaskCheckpointAndroidTest {
    @Test fun subtitlesRecoverOnlyForSameInputAndDoNotReplay() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = GeminiTaskCheckpointStore(context, GeminiTaskKind.LEGENDAS, UUID.randomUUID().toString())
        val fp = GeminiTaskRecoveryPolicy.digest("arquivoA:0-4000")
        try {
            store.start(fp, "gemini-3.5-transcribe")
            val uncertain = store.load(fp)
            assertEquals(GeminiTaskStage.REVIEW, uncertain?.stage)
            val original = listOf(SubtitleCue(text="Teste de legenda", startMs=0, endMs=1500))
            store.saveSubtitles(fp, "gemini-3.5-transcribe", original)
            val saved = store.load(fp)
            assertEquals(GeminiTaskStage.READY, saved?.stage)
            assertEquals(original[0].text, store.subtitles(saved!!)[0].text)
            assertNull(store.load(GeminiTaskRecoveryPolicy.digest("arquivoB:0-4000")))
        } finally { store.clear() }
    }

    @Test fun generatedDescriptionIsOnlyRestoredByExplicitRead() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = GeminiTaskCheckpointStore(context, GeminiTaskKind.DESCRICAO, UUID.randomUUID().toString())
        val fp = GeminiTaskRecoveryPolicy.digest("model\ncontext")
        try {
            store.start(fp, "gemini-3.5-flash-lite")
            val draft = VideoPublicationDraft("Teste privado", "#um #dois #tres #quatro #cinco")
            store.saveDescription(fp, "gemini-3.5-flash-lite", draft)
            assertEquals(draft, store.description(store.load(fp)!!))
        } finally { store.clear() }
    }
}
