package br.com.uaicortes

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** Central compartilhada: a seleção não gera áudio; catálogo/identificadores jamais são reescritos.
 * Toda síntese é voluntária, com confirmação de possível consumo da API.
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
    val aliasStore = remember(ctx) { GeminiVoiceAliases(ctx) }
    val samplePrefs = remember(ctx) { GeminiVoiceSamplePrefs(ctx) }
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
    var aliases by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sampleMode by remember { mutableStateOf(GeminiVoiceSampleText.STANDARD) }
    var customText by remember { mutableStateOf("") }
    var sampleDraftMode by remember { mutableStateOf(GeminiVoiceSampleText.STANDARD) }
    var sampleDraftText by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Todas") }
    var pitch by remember { mutableStateOf("Todos") }
    var accent by remember { mutableStateOf("") }
    var contextFilter by remember { mutableStateOf("") }
    var persona by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var onlySaved by remember { mutableStateOf(false) }
    var sampleModel by remember { mutableStateOf(initialModel) }
    var modelMenu by remember { mutableStateOf(false) }
    var storageMenu by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
    var compareOpen by remember { mutableStateOf(false) }
    var compareA by remember { mutableStateOf<String?>(null) }
    var compareB by remember { mutableStateOf<String?>(null) }
    var detailsVoice by remember { mutableStateOf<GeminiVoice?>(null) }
    var editVoice by remember { mutableStateOf<GeminiVoice?>(null) }
    var aliasDraft by remember { mutableStateOf("") }
    var pendingSample by remember { mutableStateOf<GeminiVoice?>(null) }
    var pendingDelete by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(30) }

    fun title(voice: GeminiVoice) = GeminiVoiceAliases.title(voice, aliases)
    fun sampleText(): String = GeminiVoiceSampleText.content(sampleMode, customText)
    fun sampleId(voice: GeminiVoice): String = GeminiPreviewIdentity.idForText(
        "", sampleModel, voice.id, styleInstruction, sampleText())

    suspend fun updatePreviewIndicators() {
        val keys = voices.map { it.id to sampleId(it) }
        val state = withContext(Dispatchers.IO) {
            keys.mapNotNull { (id, hash) -> id.takeIf { cache.exists(hash) } }.toSet() to cache.usageBytes()
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
                    favorites.saveNames(snapshot.voices.associate {
                        it.id to GeminiVoicePresentation.label(it)
                    })
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
        val snapshot = withContext(Dispatchers.IO) {
            Triple(library.load(), favorites.load(),
                Triple(library.autoRefresh(), library.limitMb(), cache.usageBytes()))
        }
        val personal = withContext(Dispatchers.IO) {
            Triple(aliasStore.load(), samplePrefs.mode(), samplePrefs.custom())
        }
        voices = snapshot.first.voices
        updatedAt = snapshot.first.updatedAt
        favoriteIds = snapshot.second
        autoRefresh = snapshot.third.first
        storageLimit = snapshot.third.second
        occupiedBytes = snapshot.third.third
        aliases = personal.first
        sampleMode = personal.second
        customText = personal.third
        sampleDraftMode = personal.second
        sampleDraftText = personal.third
        loaded = true
        updatePreviewIndicators()
    }
    // Atualização automática é opt-in; o padrão é funcionar somente com snapshot local.
    LaunchedEffect(loaded, autoRefresh) {
        if (loaded && autoRefresh) fetchFromGoogle()
    }
    LaunchedEffect(sampleModel, styleInstruction, voices, sampleMode, customText) {
        if (loaded) updatePreviewIndicators()
    }
    LaunchedEffect(query, gender, pitch, accent, contextFilter, persona, language, onlyFavorites, onlySaved) {
        visibleCount = 30
    }

    val matching = (GeminiVoiceSearch.byGender(GeminiVoiceSearch.filter(voices, query), gender) +
        GeminiVoiceSearch.byGender(GeminiVoiceSearch.filterWithAliases(voices, query, aliases), gender))
        .distinctBy { it.id }
    val displayed = GeminiVoiceSearch.byPitch(matching, pitch)
        .let { GeminiVoiceSearch.byMetadata(it, "accent", accent) }
        .let { GeminiVoiceSearch.byMetadata(it, "context", contextFilter) }
        .let { GeminiVoiceSearch.byMetadata(it, "persona", persona) }
        .let { GeminiVoiceSearch.byMetadata(it, "language", language) }
        .filter { (!onlyFavorites || it.id in favoriteIds) && (!onlySaved || it.id in previewIds) }
        .sortedWith(compareByDescending<GeminiVoice> { it.id in favoriteIds }
            .thenBy { GeminiVoiceAliases.title(it, aliases) })
    val visible = displayed.take(visibleCount)
    val chosenA = voices.firstOrNull { it.id == compareA }
    val chosenB = voices.firstOrNull { it.id == compareB }
    val advancedCount = listOf(accent, contextFilter, persona, language).count { it.isNotBlank() } +
        (if (gender != "Todas") 1 else 0) + (if (pitch != "Todos") 1 else 0) +
        (if (onlySaved) 1 else 0)

    fun listen(voice: GeminiVoice) {
        scope.launch {
            working = true
            try {
                // Voz e modelo/tipo de prévia devem corresponder exatamente ao cache.
                val local = withContext(Dispatchers.IO) { cache.find(sampleId(voice)) }
                if (local != null) {
                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(local)))
                    player.prepare(); player.play()
                    status = "Reproduzindo ${title(voice)} sem chamada Gemini."
                } else pendingSample = voice
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { status = "Falha ao ler prévia. Tente gerar uma nova." }
            finally { working = false }
        }
    }

    Column(modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Column(Modifier.weight(1f)) {
                Text("Central de Vozes", style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(if (onChoose == null) "Biblioteca Gemini" else "Selecionar voz",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            IconButton(enabled = !working, onClick = { scope.launch { fetchFromGoogle() } }) {
                Icon(Icons.Default.Refresh, "Atualizar catálogo manualmente")
            }
            IconButton(onClick = { sampleDraftMode = sampleMode; sampleDraftText = customText; settingsOpen = true }) {
                Icon(Icons.Default.Settings, "Configurações de prévia e armazenamento")
            }
        }
        Text(if (updatedAt > 0) "${voices.size} vozes salvas · " +
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(updatedAt))
            else "Sem catálogo salvo · Atualize para consultar o Google",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Buscar nome, apelido ou característica") },
            textStyle = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { filtersOpen = true }, shape = UaiShapes.button) {
                Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (advancedCount == 0) "Filtros" else "Filtros ($advancedCount)")
            }
            TextButton(onClick = { onlyFavorites = !onlyFavorites }) {
                Icon(if (onlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                    "Filtrar favoritas", Modifier.size(18.dp))
                Text("${favoriteIds.size}")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { compareOpen = !compareOpen }) {
                Icon(Icons.Default.CompareArrows, null, Modifier.size(18.dp))
                Text("A/B")
            }
        }
        if (compareOpen) {
            Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Comparar timbres", fontWeight = FontWeight.SemiBold)
                    Text("Use a mesma frase e modelo. Se a prévia não existir, o app pedirá confirmação.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompareVoiceButton("A", chosenA?.let(::title), !working, Modifier.weight(1f)) {
                            chosenA?.let(::listen) ?: run { status = "Escolha a voz A nos detalhes da lista." }
                        }
                        CompareVoiceButton("B", chosenB?.let(::title), !working, Modifier.weight(1f)) {
                            chosenB?.let(::listen) ?: run { status = "Escolha a voz B nos detalhes da lista." }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${displayed.size} de ${voices.size} vozes", fontSize = 12.sp,
                modifier = Modifier.weight(1f))
            if (onlySaved) Text("Com prévia · ", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary)
            Text("${occupiedBytes / (1024 * 1024)} MB em prévias", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status.isNotBlank()) Text(status, fontSize = 11.sp, maxLines = 3,
            color = MaterialTheme.colorScheme.primary)
        if (!loaded) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (displayed.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(if (voices.isEmpty()) "Nenhuma voz salva. Atualize manualmente para começar."
                    else "Nenhuma voz corresponde ao filtro.", fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(bottom = 14.dp)) {
                items(visible, key = { it.id }) { voice ->
                    val saved = voice.id in previewIds
                    Surface(onClick = { onChoose?.invoke(voice) }, enabled = onChoose != null,
                        modifier = Modifier.fillMaxWidth(), shape = UaiShapes.card,
                        color = if (voice.id == selectedVoiceId) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, null, Modifier.size(23.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title(voice), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium)
                                if (voice.id in aliases) Text(GeminiVoicePresentation.label(voice),
                                    fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GeminiVoicePresentation.shortDetails(voice), fontSize = 11.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (saved) Text("Prévia offline", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                val next = GeminiVoiceFavorites.toggle(favoriteIds, voice.id)
                                if (favorites.save(next)) favoriteIds = next
                            }, modifier = Modifier.size(40.dp)) {
                                Icon(if (voice.id in favoriteIds) Icons.Default.Star else Icons.Default.StarBorder,
                                    if (voice.id in favoriteIds) "Desfavoritar" else "Favoritar")
                            }
                            IconButton(enabled = !working, onClick = { listen(voice) },
                                modifier = Modifier.size(40.dp)) {
                                Icon(if (saved) Icons.Default.PlayCircle else Icons.Default.AutoAwesome,
                                    if (saved) "Ouvir prévia local" else "Gerar prévia (pode consumir cota)")
                            }
                            IconButton(onClick = { detailsVoice = voice }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.MoreVert, "Detalhes, apelido e comparação")
                            }
                            if (voice.id == selectedVoiceId) Icon(Icons.Default.CheckCircle,
                                "Selecionada", Modifier.size(17.dp))
                        }
                    }
                }
                if (displayed.size > visible.size) {
                    item {
                        OutlinedButton(onClick = { visibleCount += 30 }, modifier = Modifier.fillMaxWidth(),
                            shape = UaiShapes.button) {
                            Text("Mostrar mais · ${displayed.size - visible.size} restantes")
                        }
                    }
                }
            }
        }
    }

    if (filtersOpen) AlertDialog(onDismissRequest = { filtersOpen = false },
        title = { Text("Filtrar vozes") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gênero e timbre", fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Todas", "Femininas", "Masculinas").forEach { option ->
                        FilterChip(selected = gender == option, onClick = { gender = option },
                            label = { Text(option) })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Todos", "Graves", "Médias", "Agudas", "Não informado").forEach { option ->
                        FilterChip(selected = pitch == option, onClick = { pitch = option },
                            label = { Text(option) })
                    }
                }
                Text("Metadados informados pelo Google", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                MetadataFilter("Idioma", language, GeminiVoiceSearch.values(voices, "language")) { language = it }
                MetadataFilter("Sotaque", accent, GeminiVoiceSearch.values(voices, "accent")) { accent = it }
                MetadataFilter("Finalidade", contextFilter, GeminiVoiceSearch.values(voices, "context")) { contextFilter = it }
                MetadataFilter("Personalidade", persona, GeminiVoiceSearch.values(voices, "persona")) { persona = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlySaved, onCheckedChange = { onlySaved = it })
                    Text("Somente prévias salvas neste perfil")
                }
                Text("Sem inferir sotaque, idade ou identidade pela gravação.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { filtersOpen = false }) { Text("Ver resultados") } },
        dismissButton = { TextButton(onClick = {
            gender = "Todas"; pitch = "Todos"; accent = ""; persona = ""; contextFilter = ""
            language = ""; onlySaved = false
        }) { Text("Limpar filtros") } })

    if (settingsOpen) AlertDialog(onDismissRequest = { settingsOpen = false },
        title = { Text("Prévias e armazenamento") },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Atualização automática", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = autoRefresh, enabled = !working, onCheckedChange = { enabled ->
                        if (library.setAutoRefresh(enabled)) autoRefresh = enabled
                        else status = "Não foi possível gravar a preferência."
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Modelo da prévia", modifier = Modifier.weight(1f))
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
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Armazenar até", modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { storageMenu = true }) {
                            Text("$storageLimit MB")
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
                                            updatePreviewIndicators()
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
                Text("Frase para comparar vozes", fontWeight = FontWeight.Medium)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeminiVoiceSampleText.modes.forEach { mode ->
                        FilterChip(selected = sampleDraftMode == mode,
                            onClick = { sampleDraftMode = mode }, label = { Text(mode) })
                    }
                }
                if (sampleDraftMode == GeminiVoiceSampleText.CUSTOM) {
                    OutlinedTextField(value = sampleDraftText,
                        onValueChange = { if (it.length <= GeminiVoiceSampleText.MAX_CUSTOM) sampleDraftText = it },
                        label = { Text("Texto da prévia") }, minLines = 2, maxLines = 4,
                        supportingText = { Text("${sampleDraftText.length}/180 caracteres") })
                } else Text(GeminiVoiceSampleText.content(sampleDraftMode, sampleDraftText),
                    maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Gerar outra frase ou trocar de modelo pode consumir nova cota. " +
                    "Amostras existentes não são apagadas.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (occupiedBytes > 0) OutlinedButton(onClick = {
                    settingsOpen = false; pendingDelete = true
                }, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                    Icon(Icons.Default.DeleteOutline, null)
                    Text("Apagar prévias (${occupiedBytes / (1024 * 1024)} MB)")
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            if (sampleDraftMode == GeminiVoiceSampleText.CUSTOM &&
                !GeminiVoiceSampleText.customValid(sampleDraftText)) {
                status = "Escreva de 3 a 180 caracteres para a prévia personalizada."
            } else if (samplePrefs.save(sampleDraftMode, sampleDraftText)) {
                sampleMode = sampleDraftMode; customText = sampleDraftText
                settingsOpen = false
            } else status = "Não foi possível salvar as configurações da prévia."
        }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = { settingsOpen = false }) { Text("Fechar") } })

    detailsVoice?.let { voice ->
        AlertDialog(onDismissRequest = { detailsVoice = null },
            title = { Text(title(voice), maxLines = 2) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nome original: ${GeminiVoicePresentation.label(voice)}", fontSize = 12.sp)
                    Text(GeminiVoicePresentation.details(voice).ifBlank { "Sem metadados adicionais." }, fontSize = 12.sp)
                    if (voice.description.isNotBlank()) Text(voice.description, fontSize = 12.sp)
                    Text("ID: ${voice.id}", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {
                        detailsVoice = null; aliasDraft = aliases[voice.id].orEmpty(); editVoice = voice
                    }, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                        Icon(Icons.Default.Edit, null); Text("Apelido local")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { compareA = voice.id; compareOpen = true; detailsVoice = null },
                            modifier = Modifier.weight(1f), shape = UaiShapes.button) { Text("Escolher A") }
                        OutlinedButton(onClick = { compareB = voice.id; compareOpen = true; detailsVoice = null },
                            modifier = Modifier.weight(1f), shape = UaiShapes.button) { Text("Escolher B") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { detailsVoice = null }) { Text("Fechar") } })
    }
    editVoice?.let { voice ->
        AlertDialog(onDismissRequest = { editVoice = null },
            title = { Text("Apelido da voz") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Nome original: ${GeminiVoicePresentation.label(voice)}", fontSize = 12.sp)
                    OutlinedTextField(value = aliasDraft,
                        onValueChange = { if (it.length <= GeminiVoiceAliases.MAX_ALIAS_CHARS) aliasDraft = it },
                        label = { Text("Como você quer chamá-la?") }, singleLine = true)
                    Text("O apelido é privado e não altera a voz nem o ID do Gemini.", fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = {
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { aliasStore.save(voice.id, aliasDraft) }
                    if (saved != null) { aliases = saved; editVoice = null }
                    else status = "Não foi possível salvar o apelido."
                }
            }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editVoice = null }) { Text("Cancelar") } })
    }
    pendingSample?.let { voice ->
        AlertDialog(onDismissRequest = { pendingSample = null },
            title = { Text("Gerar prévia de ${title(voice)}?") },
            text = { Text("Será gerada uma amostra com a frase escolhida. Isso envia texto ao Google " +
                "e pode consumir cota ou gerar cobrança. Depois a reprodução será local até você apagar o arquivo. " +
                "Não será inserida no vídeo.") },
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
                                styleInstruction, sampleText())
                            withContext(Dispatchers.IO) { cache.save(id, wave.bytes, voice.id) }
                        }
                        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                        player.prepare(); player.play()
                        updatePreviewIndicators()
                        status = if (existing == null) "Prévia salva e reproduzida."
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
        text = { Text("Remove apenas amostras locais; não afeta áudios de projetos, catálogo ou favoritos.") },
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

@Composable
private fun CompareVoiceButton(label: String, name: String?, enabled: Boolean,
                               modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 44.dp), enabled = enabled,
        shape = UaiShapes.button) {
        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
        Text("$label: ${name ?: "Escolher"}", maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetadataFilter(title: String, selected: String, values: List<String>,
                           onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Box {
            TextButton(onClick = { expanded = true }, enabled = values.isNotEmpty()) {
                Text(selected.ifEmpty { if (values.isEmpty()) "Não informado" else "Todos" },
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Todos") }, onClick = {
                    onSelect(""); expanded = false
                })
                values.take(80).forEach { value ->
                    DropdownMenuItem(text = { Text(value, maxLines = 2) }, onClick = {
                        onSelect(value); expanded = false
                    })
                }
            }
        }
    }
}
