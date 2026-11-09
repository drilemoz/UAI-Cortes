package br.com.uaicortes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Diálogos da Home independentes da tela e do repositório de projetos. */
@Composable
internal fun CreateProjectNameDialog(name: String, onNameChange: (String)->Unit,
    onConfirm: ()->Unit, onCancel: ()->Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onCancel,
        shape = UaiShapes.dialog,
        icon = {
            Surface(color = colors.primaryContainer, shape = UaiShapes.card) {
                Icon(Icons.Default.AddCircle, null,
                    tint = colors.primary,
                    modifier = Modifier.padding(10.dp))
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Novo projeto", style = MaterialTheme.typography.headlineSmall)
                Text("Escolha um nome inicial para organizar sua edição.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it.take(80)) },
                    label = { Text("Nome do projeto") },
                    placeholder = { Text("Meu vídeo") },
                    singleLine = true,
                    isError = !ProjectNaming.validTitle(name),
                    supportingText = {
                        Text(if (ProjectNaming.validTitle(name))
                            "Você poderá renomear depois, sem perder o conteúdo."
                        else "Digite um nome válido com até 80 caracteres.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm,
                enabled = ProjectNaming.validTitle(name),
                shape = UaiShapes.button) { Text("Escolher vídeos") }
        },
        dismissButton = { TextButton(onClick = onCancel, shape = UaiShapes.button) { Text("Cancelar") } })
}


/** Renomeia somente o título. Nome, clipes e mídias são gravados pelo repositório. */
@Composable
internal fun RenameProjectDialog(currentTitle: String, name: String,
    onNameChange: (String) -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val normalized = ProjectNaming.title(name)
    AlertDialog(onDismissRequest = onCancel, shape = UaiShapes.dialog,
        title = { Text("Renomear projeto") },
        text = {
            OutlinedTextField(value = name, onValueChange = { onNameChange(it.take(80)) },
                modifier = Modifier.fillMaxWidth().testTag("rename_project_input"),
                label = { Text("Novo nome") }, singleLine = true,
                isError = !ProjectNaming.validTitle(name),
                supportingText = { Text("Até 80 caracteres. O conteúdo do projeto não será alterado.") })
        },
        confirmButton = {
            TextButton(onClick = onConfirm,
                enabled = ProjectNaming.validTitle(name) && normalized != currentTitle) {
                Text("Salvar nome")
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } })
}

@Composable
internal fun DeleteAllProjectsDialog(count: Int, onConfirm: ()->Unit, onCancel: ()->Unit) {
    AlertDialog(onDismissRequest=onCancel,
        icon={Icon(Icons.Default.DeleteSweep,null)},
        title={Text("Excluir todos os projetos?")},
        text={Text("Os $count projetos salvos serão excluídos, inclusive os que não aparecem nas edições recentes. Os vídeos originais e MP4s exportados serão mantidos. Não é possível desfazer.")},
        confirmButton={TextButton(onClick=onConfirm,enabled=count>0) {
            Text("Excluir $count projetos",color=MaterialTheme.colorScheme.error)
        }},
        dismissButton={TextButton(onClick=onCancel) {Text("Cancelar")}})
}
