package br.com.uaicortes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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
        var animateCalls = 0
        var flowCalls = 0
        var updatesCalls = 0
        rule.setContent {
            var route by remember { mutableStateOf("home") }
            MaterialTheme {
                if (route == "home") {
                    MainHome(emptyList(), onCreate = { createCalls++ }, onAnimate = { animateCalls++ },
                        onOpen = {}, onDelete = {}, onDeleteAll = {},
                        onSettings = { route = "about" }, onTab = {},
                        onFlow = { flowCalls++ })
                } else {
                    AboutScreen(back = { route = "home" }, onUpdates = { updatesCalls++ })
                }
            }
        }
        val version = "Versão ${AppReleaseNotes.VERSION_NAME} (${AppReleaseNotes.VERSION_CODE})"
        rule.onNodeWithText(version).assertExists()
        rule.onNodeWithText("Novo projeto").assertExists()
        rule.onNodeWithText("Animar fotos").assertExists()
        rule.onNodeWithText("Estúdio de Cenas").assertExists()
        rule.onNodeWithContentDescription("Configurações").performClick()
        rule.onNodeWithText(version).assertExists()
        rule.onNodeWithText("Verificar atualizações").performClick()
        rule.onNodeWithText("Ver novidades").assertDoesNotExist()
        rule.onNodeWithText("Testar visual da Home").assertDoesNotExist()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(0, createCalls)
            org.junit.Assert.assertEquals(0, animateCalls)
            org.junit.Assert.assertEquals(0, flowCalls)
            org.junit.Assert.assertEquals(1, updatesCalls)
        }
    }

    @Test fun eachHomeShortcutKeepsItsOriginalNavigation() {
        var createCalls = 0
        var animateCalls = 0
        var flowCalls = 0
        rule.setContent {
            MaterialTheme {
                HomePrimaryActions(onCreate = { createCalls++ },
                    onAnimate = { animateCalls++ }, onFlow = { flowCalls++ })
            }
        }
        rule.onNodeWithText("Novo projeto").performClick()
        rule.onNodeWithText("Animar fotos").performClick()
        rule.onNodeWithText("Estúdio de Cenas").performClick()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(1, createCalls)
            org.junit.Assert.assertEquals(1, animateCalls)
            org.junit.Assert.assertEquals(1, flowCalls)
        }
    }
}
