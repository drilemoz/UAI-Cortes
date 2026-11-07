package br.com.uaicortes

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Central única acessível pela Home e como seletor de Vozes IA.
 * Catálogo, favoritos e WAVs são compartilhados; a abertura não faz rede por padrão.
 */
@Composable
internal fun GeminiVoiceCenterScreen(
    onBack: () -> Unit,
    onChoose: ((GeminiVoice) -> Unit)? = null,
    selectedVoiceId: String? = null,
    initialModel: String = GeminiModels.LITE,
    onModelChange: (String) -> Unit = {},
    styleInstruction: String = GeminiNarrationStyling.instruction("Neutra"),
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val library = remember(ctx) { GeminiVoiceLibrary(ctx) }
    val cache = remember(ctx) { GeminiVoicePreviewCache(ctx) }
    val favorites = remember(ctx) { GeminiVoiceFavorites(ctx) }
    val credentials = remember(ctx) { GeminiCredentialStore(ctx) }
    val pacing = remember(ctx) { GeminiLocalPacing(ctx) }
    val client = remember(ctx) { GeminiTtsClient(ctx) }
    val player = remember(ctx) { diagnosticPlayer(ctx) }
    DisposableEffect(client, player) {
        onDispose { client.cancel(); player.release() }
    }

    var loaded by remember { mutableStateOf(false) }
    var voices by remember { mutableStateOf<List<GeminiVoice>>(emptyList()) }
    var updatedAt by remember { mutableLongStateOf(0L) }
    var autoRefresh by remember { mutableStateOf(false) }
    var storageLimit by remember { mutableIntStateOf(100) }
    var occupiedBytes by remember { mutableLongStateOf(0L) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var previewIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Todas") }
    var pitch by remember { mutableStateOf("Todos") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var sampleModel by remember { mutableStateOf(initialModel) }
    var modelMenu by remember { mutableStateOf(false) }
    var storageMenu by remember { mutableStateOf(false) }
    var pendingSample by remember { mutableStateOf<GeminiVoice?>(null) }
    var pendingDelete by remember { mutableStateOf(false) }

    fun sampleId(voice: GeminiVoice): String = GeminiPreviewIdentity.id("", sampleModel, voice.id, styleInstruction)
    suspend fun updatePreviewIndicators() {
        val keys = voices.map { it.id to sampleId(it) }
        val state = withContext(Dispatchers.IO) {
            keys.mapNotNull { (voiceId, hash) -> voiceId.takeIf { cache.exists(hash) } }.toSet() to
                cache.usageBytes()
        }
        previewIds = state.first
        occupiedBytes = state.second
    }
    suspend fun fetchFromGoogle() {
        if (working) return
        working = true
        try {
            val key = withContext(Dispatchers.IO) { credentials.read() }
                ?: throw IllegalStateException("Configure uma chave Gemini em Serviços e APIs.")
            val fetched = client.voicesWithDiagnosis(key)
            if (fetched.voices.isEmpty()) {
                status = "O Google retornou uma lista vazia. Seu catálogo anterior foi preservado."
            } else {
                val saved = withContext(Dispatchers.IO) {
                    val snapshot = library.save(fetched.voices)
                    favorites.saveNames(snapshot.voices.associate { it.id to GeminiVoicePresentation.label(it) })
                    snapshot
                }
                voices = saved.voices
                updatedAt = saved.updatedAt
                updatePreviewIndicators()
                status = "Catálogo atualizado: ${saved.voices.size} vozes. Nenhuma prévia foi apagada."
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            status = "Não foi possível atualizar: ${GeminiVoiceCatalog.safeMessage(e)} " +
                "O catálogo salvo continua disponível. Verifique a conexão/chave ou tente novamente."
        } finally { working = false }
    }
    LaunchedEffect(Unit) {
        val initial = withContext(Dispatchers.IO) {
            Triple(library.load(), favorites.load(), Triple(library.autoRefresh(), library.limitMb(), cache.usageBytes()))
        }
        voices = initial.first.voices
        updatedAt = initial.first.updatedAt
        favoriteIds = initial.second
        autoRefresh = initial.third.first
        storageLimit = initial.third.second
        occupiedBytes = initial.third.third
        loaded = true
        updatePreviewIndicators()
    }
    // Atualização automática é opt-in; o padrão é funcionar somente com snapshot local.
    LaunchedEffect(loaded, autoRefresh) {
        if (loaded && autoRefresh) fetchFromGoogle()
    }
    LaunchedEffect(sampleModel, styleInstruction, voices) { if (loaded) updatePreviewIndicators() }

    val displayed = GeminiVoiceSearch.byPitch(
        GeminiVoiceSearch.byGender(GeminiVoiceSearch.filter(voices, query), gender), pitch)
        .filter { !onlyFavorites || it.id in favoriteIds }
        .sortedWith(compareByDescending<GeminiVoice> { it.id in favoriteIds }
            .thenBy { GeminiVoicePresentation.label(it) })

    Column(modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Column(Modifier.weight(1f)) {
                Text("Central de Vozes", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(if (onChoose == null) "Biblioteca Google · disponível sem projeto" else
                    "Escolha uma voz para o projeto", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(enabled = !working, onClick = { scope.launch { fetchFromGoogle() } }) {
                Icon(Icons.Default.Refresh, "Atualizar catálogo manualmente")
            }
        }
        Text(if (updatedAt > 0) "${voices.size} vozes salvas · atualizado em " +
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(updatedAt))
            else "Nenhum catálogo salvo. Toque em Atualizar para consultar o Google.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Atualização automática", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = autoRefresh, enabled = !working, onCheckedChange = { enabled ->
                if (library.setAutoRefresh(enabled)) autoRefresh = enabled
                else status = "Não foi possível gravar a preferência."
            }, modifier = Modifier.height(34.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Modelo da prévia", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { modelMenu = true }) {
                    Text(if (sampleModel == GeminiModels.LITE) "Lite" else "Flash")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                    GeminiModels.supported.forEach { candidate ->
                        DropdownMenuItem(text = { Text(if (candidate == GeminiModels.LITE) "Lite" else "Flash") },
                            onClick = { sampleModel = candidate; onModelChange(candidate); modelMenu = false })
                    }
                }
            }
            Text("Limite", fontSize = 12.sp)
            Box {
                TextButton(onClick = { storageMenu = true }) {
                    Text("${storageLimit} MB")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = storageMenu, onDismissRequest = { storageMenu = false }) {
                    listOf(50, 100, 250).forEach { amount ->
                        DropdownMenuItem(text = { Text("$amount MB") }, onClick = {
                            storageMenu = false
                            scope.launch {
                                if (withContext(Dispatchers.IO) {
                                    library.setLimitMb(amount).also { if (it) cache.prune() }
                                }) {
                                    storageLimit = amount
                                    occupiedBytes = withContext(Dispatchers.IO) { cache.usageBytes() }
                                    updatePreviewIndicators()
                                }
                            }
                        })
                    }
                }
            }
        }
        Text("Prévias: ${occupiedBytes / 1024 / 1024} MB salvos · frase fixa: " +
            "\"${GeminiPreviewIdentity.TEXT}\"", fontSize = 11.sp,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Buscar voz ou característica") },
            textStyle = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todas", "Femininas", "Masculinas").forEach { option ->
                FilterChip(selected = gender == option, onClick = { gender = option }, label = { Text(option) })
            }
            listOf("Todos", "Graves", "Médias", "Agudas", "Não informado").forEach { option ->
                FilterChip(selected = pitch == option, onClick = { pitch = option },
                    label = { Text(option) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${displayed.size} de ${voices.size} vozes", fontSize = 12.sp,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { onlyFavorites = !onlyFavorites }) {
                Icon(if (onlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                    "Filtrar favoritas", Modifier.size(17.dp))
                Text("Favoritas (${favoriteIds.size})", fontSize = 11.sp)
            }
            if (occupiedBytes > 0) IconButton(onClick = { pendingDelete = true }) {
                Icon(Icons.Default.DeleteOutline, "Excluir prévias armazenadas")
            }
        }
        if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status.isNotBlank()) Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        if (!loaded) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (displayed.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (voices.isEmpty()) "Nenhuma voz salva. Atualize manualmente para começar."
                    else "Nenhuma voz corresponde ao filtro.", fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(bottom = 14.dp)) {
                items(displayed, key = { it.id }) { voice ->
                    val saved = voice.id in previewIds
                    Surface(onClick = { onChoose?.invoke(voice) },
                        enabled = onChoose != null, modifier = Modifier.fillMaxWidth(),
                        shape = UaiShapes.card,
                        color = if (voice.id == selectedVoiceId) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, null, Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(GeminiVoicePresentation.label(voice), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                Text(GeminiVoicePresentation.shortDetails(voice), fontSize = 11.sp,
                                    maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (saved) Text("Prévia salva · ouvir sem API", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                val next = GeminiVoiceFavorites.toggle(favoriteIds, voice.id)
                                if (favorites.save(next)) favoriteIds = next
                            }) {
                                Icon(if (voice.id in favoriteIds) Icons.Default.Star else Icons.Default.StarBorder,
                                    if (voice.id in favoriteIds) "Desfavoritar" else "Favoritar")
                            }
                            IconButton(enabled = !working, onClick = {
                                scope.launch {
                                    working = true
                                    try {
                                        val local = withContext(Dispatchers.IO) { cache.find(sampleId(voice)) }
                                        if (local != null) {
                                            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(local)))
                                            player.prepare(); player.play()
                                            status = "Reproduzindo ${GeminiVoicePresentation.label(voice)} sem chamada Gemini."
                                        } else pendingSample = voice
                                    } catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { status = "Falha ao ler prévia. Tente gerar uma nova." }
                                    finally { working = false }
                                }
                            }) {
                                Icon(if (saved) Icons.Default.PlayCircle else Icons.Default.AutoAwesome,
                                    if (saved) "Ouvir prévia local" else "Gerar prévia (pode consumir cota)")
                            }
                            if (voice.id == selectedVoiceId) Icon(Icons.Default.CheckCircle,
                                "Selecionada", Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
    pendingSample?.let { voice ->
        AlertDialog(onDismissRequest = { pendingSample = null },
            title = { Text("Gerar prévia de ${GeminiVoicePresentation.label(voice)}?") },
            text = { Text("Será gerada uma única amostra com a frase padrão. " +
                "Isso envia texto ao Google e pode consumir cota ou gerar cobrança. " +
                "Depois a reprodução será local até você apagar o arquivo. Não será inserida no vídeo.") },
            confirmButton = { TextButton(onClick = {
                pendingSample = null
                scope.launch {
                    working = true
                    try {
                        val id = sampleId(voice)
                        val existing = withContext(Dispatchers.IO) { cache.find(id) }
                        val file = if (existing != null) existing else {
                            val key = withContext(Dispatchers.IO) { credentials.read() }
                                ?: error("Chave Gemini ausente.")
                            withContext(Dispatchers.IO) { pacing.beforeRequest(sampleModel) { } }
                            val wave = client.synthesize(key, sampleModel, voice.id,
                                styleInstruction, GeminiPreviewIdentity.TEXT)
                            withContext(Dispatchers.IO) { cache.save(id, wave.bytes, voice.id) }
                        }
                        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                        player.prepare(); player.play()
                        updatePreviewIndicators()
                        status = if (existing == null) "Prévia salva no aparelho e reproduzida."
                            else "Prévia local reutilizada; sem nova chamada."
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        status = "Prévia não concluída: ${GeminiVoiceCatalog.safeMessage(e)} " +
                            "Se a requisição chegou ao Google, poderá ter consumido cota."
                    } finally { working = false }
                }
            }) { Text("Gerar e salvar") } },
            dismissButton = { TextButton(onClick = { pendingSample = null }) { Text("Cancelar") } })
    }
    if (pendingDelete) AlertDialog(onDismissRequest = { pendingDelete = false },
        title = { Text("Excluir prévias salvas?") },
        text = { Text("Isso remove apenas as amostras de voz locais, não os áudios inseridos nos projetos, nem o catálogo e favoritos.") },
        confirmButton = { TextButton(onClick = {
            pendingDelete = false
            scope.launch {
                val removed = withContext(Dispatchers.IO) { cache.clear() }
                updatePreviewIndicators()
                status = "$removed prévias locais removidas."
            }
        }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { pendingDelete = false }) { Text("Cancelar") } })
}
