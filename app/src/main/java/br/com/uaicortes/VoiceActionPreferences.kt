package br.com.uaicortes

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

internal class VoiceActionPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("voice_action_prompts_v1", Context.MODE_PRIVATE)

    enum class Action(val key: String) {
        PREVIEW("skip_preview"),
        INSERT_VOICE("skip_insert_voice"),
        INSERT_BATCH("skip_insert_batch")
    }

    fun skips(action: Action): Boolean = prefs.getBoolean(action.key, false)
    fun setSkipping(action: Action, skip: Boolean) {
        prefs.edit().putBoolean(action.key, skip).apply()
    }
}

@Composable
internal fun VoiceSkipPromptOption(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text("Não mostrar novamente", style = MaterialTheme.typography.bodyMedium)
    }
}
