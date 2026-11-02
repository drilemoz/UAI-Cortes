package br.com.uaicortes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Confere a mesma versão Home/Sobre e ações reais em prévia somente leitura. */
@RunWith(AndroidJUnit4::class)
class HomeAboutPreviewAndroidTest {
    @get:Rule val rule = createComposeRule()

    @Test fun homeAndAboutReuseVersionAndPrimaryActionsWithoutSideEffects() {
        var createCalls = 0
        var animateCalls = 0
        rule.setContent {
            var route by remember { mutableStateOf("home") }
            MaterialTheme {
                if (route == "home") {
                    MainHome(emptyList(), onCreate = { createCalls++ }, onAnimate = { animateCalls++ },
                        onOpen = {}, onDelete = {}, onDeleteAll = {},
                        onSettings = { route = "about" }, onTab = {})
                } else {
                    AboutScreen(back = { route = "home" }, onShowWhatsNew = {})
                }
            }
        }
        val version = "Versão ${AppReleaseNotes.VERSION_NAME} (${AppReleaseNotes.VERSION_CODE})"
        rule.onNodeWithText(version).assertExists()
        rule.onNodeWithText("Nova edição").assertExists()
        rule.onNodeWithText("Criar projeto").assertExists()
        rule.onNodeWithText("Animar Imagens").assertExists()
        rule.onNodeWithContentDescription("Configurações").performClick()
        rule.onNodeWithText(version).assertExists()
        rule.onNodeWithText("Testar visual da Home").performClick()
        rule.onNodeWithText("Nova edição").assertExists()
        rule.onNodeWithText("Criar projeto").assertIsNotEnabled()
        rule.onNodeWithText("Animar Imagens").assertIsNotEnabled()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(0, createCalls)
            org.junit.Assert.assertEquals(0, animateCalls)
        }
    }
}
