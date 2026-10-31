package br.com.uaicortes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Componentes visuais da tela Áudio. Não criam trilhas ou alteram o modelo. */
@Composable
internal fun AudioPanelCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,
        tonalElevation=2.dp,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
        modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}

@Composable
internal fun AudioTrackCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape=RoundedCornerShape(14.dp),
        color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.35f),
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
        modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal=10.dp,vertical=6.dp),content=content)
    }
}

@Composable
internal fun AudioQuickAction(label:String,icon:ImageVector,onClick:()->Unit,
                              modifier:Modifier=Modifier,enabled:Boolean=true) {
    OutlinedButton(onClick=onClick,modifier=modifier,enabled=enabled,
        contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {
        Icon(icon,null,modifier=Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(label,fontSize=12.sp,maxLines=1)
    }
}

