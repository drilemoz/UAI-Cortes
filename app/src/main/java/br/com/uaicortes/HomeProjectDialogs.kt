package br.com.uaicortes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
