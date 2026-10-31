package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun releaseSymbol(symbol: String): ImageVector = when (symbol) {
    "film" -> Icons.Default.Movie
    "folder" -> Icons.Default.FolderOpen
    "layers" -> Icons.Default.Layers
    "captions" -> Icons.Default.Subtitles
    "shield" -> Icons.Default.VerifiedUser
    "sync" -> Icons.Default.Sync
    else -> Icons.Default.AutoAwesome
}

/** Componente compartilhado: nenhuma nota é digitada separadamente na seção Sobre. */
@Composable
internal fun ReleaseHighlights(entries: List<ReleaseSummary>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        entries.forEachIndexed { index, entry ->
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (index == 0) "Nesta versão" else "Atualização anterior",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("v${entry.version}", Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                entry.features.forEach { feature ->
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp) {
                        Row(Modifier.fillMaxWidth().padding(15.dp),
                            verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center) {
                                Icon(releaseSymbol(feature.symbol), contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(23.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(feature.title, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge)
                                Text(feature.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WhatsNewScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0866FF), Color(0xFF5841AE))))) {
                Column(Modifier.fillMaxWidth().padding(25.dp),
                    horizontalAlignment = Alignment.Start) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Tem novidade por aqui!", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 26.sp,
                        lineHeight = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Conheça o que mudou no Uai Cortes.",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(14.dp))
                    Text("VERSÃO ${AppReleaseNotes.VERSION_NAME}",
                        color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
            ReleaseHighlights(AppReleaseNotes.forWhatsNew())
            Spacer(Modifier.height(24.dp))
            Text("Você pode rever estas informações em Configurações → Novidades ou Sobre.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Button(onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                .height(54.dp),
            shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text("Continuar", fontWeight = FontWeight.SemiBold)
        }
    }
}
