package br.com.uaicortes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** O Sobre oferece somente atualizações; atalhos da Home continuam no próprio painel. */
@RunWith(AndroidJUnit4::class)
class HomeAboutPreviewAndroidTest {
    @get:Rule val rule = createComposeRule()

    @Test fun homeAndAboutReuseVersionAndPrimaryActionsWithoutSideEffects() {
        var createCalls = 0
        var updatesCalls = 0
        rule.setContent {
            var route by remember { mutableStateOf("home") }
            MaterialTheme {
                if (route == "home") {
                    MainHome(emptyList(), onCreate = { createCalls++ },
                        onOpen = {}, onRename = {}, onDelete = {}, onDeleteAll = {},
                        onSettings = { route = "about" }, onTab = {})
                } else {
                    AboutScreen(back = { route = "home" }, onUpdates = { updatesCalls++ })
                }
            }
        }
        val version = "Versão ${AppReleaseNotes.VERSION_NAME} (${AppReleaseNotes.VERSION_CODE})"
        rule.onNodeWithText(version).assertExists()
        rule.onNodeWithText("Novo projeto").assertExists()
        rule.onNodeWithText("Animar fotos").assertDoesNotExist()
        rule.onNodeWithText("Estúdio de Cenas").assertDoesNotExist()
        rule.onNodeWithContentDescription("Configurações").performClick()
        rule.onNodeWithText(version).assertExists()
        rule.onNodeWithText("Verificar atualizações").performClick()
        rule.onNodeWithText("Ver novidades").assertDoesNotExist()
        rule.onNodeWithText("Testar visual da Home").assertDoesNotExist()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(0, createCalls)
            org.junit.Assert.assertEquals(1, updatesCalls)
        }
    }

    @Test fun sixHomeShortcutsScrollAndKeepIndividualNavigation() {
        val calls = IntArray(6)
        rule.setContent {
            MaterialTheme {
                HomePrimaryActions(
                    onCreate = { calls[0]++ },
                    onVoiceCenter = { calls[1]++ },
                    onTextToVoice = { calls[2]++ },
                    onEditAudio = { calls[3]++ },
                    onQuickAudio = { calls[4]++ },
                    onQuickSplit = { calls[5]++ })
            }
        }
        listOf("Novo projeto", "Vozes Google",
            "Texto para Voz", "Editar áudio", "Extrair áudio", "Dividir vídeo").forEach { label ->
            rule.onNodeWithText(label).performScrollTo().performClick()
        }
        rule.runOnIdle { org.junit.Assert.assertArrayEquals(IntArray(6) { 1 }, calls) }
    }
}
