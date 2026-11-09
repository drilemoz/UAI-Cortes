package br.com.uaicortes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Diálogos da Home independentes da tela e do repositório de projetos. */
@Composable
internal fun CreateProjectNameDialog(name: String, onNameChange: (String)->Unit,
    onConfirm: ()->Unit, onCancel: ()->Unit) {
    AlertDialog(onDismissRequest=onCancel,
        title={Text("Nome do novo projeto")},
        text={OutlinedTextField(value=name, onValueChange={onNameChange(it.take(80))},
            label={Text("Nome do projeto")}, singleLine=true,
            isError=!ProjectNaming.validTitle(name), modifier=Modifier.fillMaxWidth())},
        confirmButton={TextButton(onClick=onConfirm, enabled=ProjectNaming.validTitle(name)) {
            Text("Escolher vídeos")
        }},
        dismissButton={TextButton(onClick=onCancel) { Text("Cancelar") }})
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
