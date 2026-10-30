package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Controle compacto reutilizável com os mesmos chips e dimensões nas opções do editor. */
@Composable
internal fun <T> EditorOptionRow(label: String, choices: List<T>, selected: T, onChoose: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,style=MaterialTheme.typography.labelMedium,modifier=Modifier.width(88.dp))
        LazyRow(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp),
            contentPadding=PaddingValues(end=8.dp)) {
            items(choices) { choice ->
                FilterChip(selected=selected==choice,onClick={onChoose(choice)},
                    label={Text(choice.toString(),fontSize=12.sp,maxLines=1)})
            }
        }
    }
}
