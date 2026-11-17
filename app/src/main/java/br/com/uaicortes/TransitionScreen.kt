package br.com.uaicortes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Aproxima a tintura das transições na prévia do ExoPlayer.
 * Para ver a composição exata, use a prévia renderizada da junção;
 * a renderização em vídeo usa Media3 no MP4 exportado.
 */
@Composable
internal fun TransitionPreviewTint(incoming: ClipTransition?, incomingMs: Long,
    outgoing: ClipTransition?, outgoingMs: Long, localMs: Long, clipDurationMs: Long) {
    val incomingKind=incoming?.kind ?: TransitionEffects.CUT
    val outgoingKind=outgoing?.kind ?: TransitionEffects.CUT
    val at = localMs.coerceIn(0L,clipDurationMs.coerceAtLeast(0L))
    val inStrength = if (incomingMs > 0L) TransitionEffects.intensity(incomingKind,
        ((incomingMs - at).toFloat() / incomingMs).coerceIn(0f,1f)) else 0f
    val outStrength = if (outgoingMs > 0L) TransitionEffects.intensity(outgoingKind,
        ((at - (clipDurationMs - outgoingMs)).toFloat() / outgoingMs).coerceIn(0f,1f)) else 0f
    val kind = if (inStrength >= outStrength) incomingKind else outgoingKind
    val strength = maxOf(inStrength,outStrength)
    val tint = when (kind) {
        TransitionEffects.BLACK,TransitionEffects.FLASH_BLACK -> Color.Black
        TransitionEffects.WHITE,TransitionEffects.FLASH_WHITE -> Color.White
        TransitionEffects.BLUE -> Color(0xFF1448F2)
        TransitionEffects.GOLD -> Color(0xFFFFAD2E)
        else -> null
    }
    if (tint != null && strength > .001f)
        Box(Modifier.fillMaxSize().background(tint.copy(alpha=strength)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransitionScreen(p: VideoProject, back: () -> Unit, update: (VideoProject) -> Boolean,
    initialBoundary: Int = 0) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = back, sheetState = sheet) {
        if (p.clips.size < 2) {
            Text("Adicione dois clipes para editar a passagem entre eles.",
                Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp))
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            var boundary by remember(p.id, initialBoundary) {
                mutableIntStateOf(TransitionBoundaryPolicy.clamp(initialBoundary, p.clips.size))
            }
            val safeBoundary = TransitionBoundaryPolicy.clamp(boundary, p.clips.size)
            val from = p.clips[safeBoundary]
            val to = p.clips[safeBoundary + 1]
            val stored = EditFeatures.boundary(p, safeBoundary)
            var kind by remember(p.id, from.id, to.id) {
                mutableStateOf(stored?.kind?.takeIf(TransitionEffects::supported) ?: TransitionEffects.CUT)
            }
            var durationMs by remember(p.id, from.id, to.id) {
                mutableLongStateOf(stored?.durationMs ?: 300L)
            }
            var showMore by remember { mutableStateOf(false) }
            var applyAll by remember { mutableStateOf(false) }
            var audioOn by remember { mutableStateOf(false) }
            var outputMode by remember { mutableStateOf(PreviewOutputMode.ORIGINAL) }
            var saveFailed by remember { mutableStateOf(false) }
            val first = p.clips.first()
            val aspect by produceState<Float?>(null, first.uri, first.isImage) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    mediaAspect(context, first.uri, first.isImage)
                }
            }
            val modern = JunctionTransitionPlan.contains(kind)
            val join = JunctionTransitionPlan.boundary(from, to, from.durationMs, kind, durationMs)
            val allValid = !modern || !applyAll || p.clips.zipWithNext().all { (a, b) ->
                JunctionTransitionPlan.boundary(a, b, a.durationMs, kind, durationMs) != null
            }
            val valid = (!modern || join != null) && allValid
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(horizontal = 16.dp)) {
                Text("Transição", style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { boundary = safeBoundary - 1 }, enabled = safeBoundary > 0) {
                        Icon(Icons.Default.ChevronLeft, "Junção anterior")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Junção ${safeBoundary + 1} de ${p.clips.lastIndex}",
                            style = MaterialTheme.typography.labelLarge)
                        Text("${from.title} → ${to.title}", maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { boundary = safeBoundary + 1 }, enabled = safeBoundary < p.clips.lastIndex - 1) {
                        Icon(Icons.Default.ChevronRight, "Próxima junção")
                    }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreviewOutputMode.entries.forEach { mode ->
                            FilterChip(outputMode == mode, { outputMode = mode }, label = { Text(mode.label) })
                        }
                    }
                    val renderAspect = aspect
                    if ((!modern || join != null) && renderAspect != null) {
                        key(p, safeBoundary, kind, durationMs, renderAspect, outputMode) {
                            TransitionVideoPreview(p, safeBoundary, kind, durationMs, renderAspect, outputMode, audioOn) {
                                audioOn = it
                            }
                        }
                    } else if (modern && join == null) {
                        Text("Os clipes são curtos demais para esta transição.",
                            color = MaterialTheme.colorScheme.error)
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(kind == TransitionEffects.CUT, { kind = TransitionEffects.CUT },
                            label = { Text("Corte direto") })
                        FilterChip(kind == JunctionTransitionPlan.DISSOLVE, {
                            kind = JunctionTransitionPlan.DISSOLVE
                            durationMs = stored?.takeIf { it.kind == kind }?.durationMs ?: 300L
                        }, label = { Text("Dissolver") })
                        FilterChip(kind in listOf(JunctionTransitionPlan.PUSH_LEFT, JunctionTransitionPlan.PUSH_RIGHT), {
                            kind = stored?.kind?.takeIf { it == JunctionTransitionPlan.PUSH_LEFT ||
                                it == JunctionTransitionPlan.PUSH_RIGHT } ?: JunctionTransitionPlan.PUSH_LEFT
                            durationMs = stored?.takeIf { it.kind == kind }?.durationMs ?: 250L
                        }, label = { Text("Empurrão lateral") })
                        FilterChip(kind == TransitionEffects.BLACK, {
                            kind = TransitionEffects.BLACK
                            durationMs = stored?.takeIf { it.kind == kind }?.durationMs ?: 400L
                        }, label = { Text("Fade preto") })
                    }
                    if (kind == JunctionTransitionPlan.PUSH_LEFT || kind == JunctionTransitionPlan.PUSH_RIGHT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(kind == JunctionTransitionPlan.PUSH_LEFT,
                                { kind = JunctionTransitionPlan.PUSH_LEFT }, label = { Text("Para esquerda") })
                            FilterChip(kind == JunctionTransitionPlan.PUSH_RIGHT,
                                { kind = JunctionTransitionPlan.PUSH_RIGHT }, label = { Text("Para direita") })
                        }
                    }
                    if (kind != TransitionEffects.CUT) {
                        Text(TransitionEffects.description(kind), style = MaterialTheme.typography.bodySmall)
                        val effective = join?.durationMs ?: if (!modern)
                            2L * EditFeatures.transitionMs(ClipTransition(from.id, to.id, kind,
                                durationMs.coerceIn(250L, 1000L)), from.durationMs, to.durationMs)
                        else 0L
                        Text("Duração na junção · $effective ms", style = MaterialTheme.typography.labelLarge)
                        val range = if (modern) 120f..1000f else 250f..1000f
                        var slidingMs by remember(kind, durationMs) { mutableFloatStateOf(durationMs.toFloat()) }
                        Slider(value = slidingMs.coerceIn(range.start, range.endInclusive), valueRange = range,
                            onValueChange = { slidingMs = it }, onValueChangeFinished = {
                                durationMs = ((slidingMs.toLong() + 5L) / 10L) * 10L
                            })
                        if (join?.usesHold == true) Text(
                            "Sem sobra de vídeo num dos lados. Um quadro de apoio é usado nesse lado.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Mantém a duração, as legendas e os áudios no mesmo horário.",
                        style = MaterialTheme.typography.bodySmall)
                    if (modern) Text("As transições combinadas são exportadas em SDR.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { showMore = !showMore }) {
                        Text(if (showMore) "Recolher efeitos" else "Mais efeitos")
                    }
                    if (showMore) {
                        for (row in TransitionEffects.names.filter {
                            it != TransitionEffects.CUT && it != TransitionEffects.BLACK
                        }.chunked(2)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (effect in row) FilterChip(kind == effect, {
                                    kind = effect
                                    durationMs = stored?.takeIf { it.kind == kind }?.durationMs ?: 350L
                                }, label = { Text(effect) }, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(applyAll, { applyAll = it })
                            Text("Aplicar também às outras junções")
                        }
                    }
                }
                if (!allValid) Text("Há junções curtas demais. Aplique somente nesta junção.",
                    color = MaterialTheme.colorScheme.error)
                if (saveFailed) Text("Não foi possível salvar. Tente novamente.",
                    color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    val amended = TransitionBatch.apply(p, safeBoundary, kind, durationMs, applyAll)
                    if (amended == p || update(amended)) back() else saveFailed = true
                }, enabled = valid, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (applyAll) "Aplicar a todas as junções" else "Aplicar nesta junção")
                }
            }
        }
    }
}
