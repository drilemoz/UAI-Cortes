package br.com.uaicortes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Contrato de interação em Android: projetos recentes e aba Projetos compartilham o rename. */
@RunWith(AndroidJUnit4::class)
class HomeRenameProjectAndroidTest {
    @get:Rule val rule = createComposeRule()

    @Test fun renameDialogChangesOnlyTitleAfterConfirmation() {
        var renamed: String? = null
        val base = VideoProject(id = "sample", title = "Cenas antigas")
        rule.setContent {
            var selected by remember { mutableStateOf<VideoProject?>(null) }
            var value by remember { mutableStateOf("") }
            MaterialTheme {
                MainHome(listOf(base), onCreate = {}, onAnimate = {}, onOpen = {},
                    onRename = { selected = it; value = it.title }, onDelete = {},
                    onDeleteAll = {}, onSettings = {}, onTab = {})
                selected?.let { project ->
                    RenameProjectDialog(project.title, value, { value = it },
                        onConfirm = { renamed = ProjectNaming.title(value); selected = null },
                        onCancel = { selected = null })
                }
            }
        }
        rule.onNodeWithContentDescription("Renomear Cenas antigas").performClick()
        rule.onNodeWithTag("rename_project_input").performTextClearance()
        rule.onNodeWithTag("rename_project_input").performTextInput("  Reeditado ")
        rule.onNodeWithText("Salvar nome").performClick()
        rule.runOnIdle {
            assertEquals("Reeditado", renamed)
            assertEquals("Cenas antigas", base.title)
        }
    }
}
