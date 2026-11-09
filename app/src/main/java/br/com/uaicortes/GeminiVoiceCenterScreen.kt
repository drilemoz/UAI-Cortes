package br.com.uaicortes

import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.DateFormat
import java.util.Date

private data class VoiceCenterFilterResult(val voices: List<GeminiVoice>, val elapsedMs: Long)

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
    val reviewStore = remember(ctx) { GeminiVoiceReviewStore(ctx) }
    val samplePrefs = remember(ctx) { GeminiVoiceSamplePrefs(ctx) }
    val credentials = remember(ctx) { GeminiCredentialStore(ctx) }
    val pacing = remember(ctx) { GeminiLocalPacing(ctx) }
    val client = remember(ctx) { GeminiTtsClient(ctx) }
    val player = remember(ctx) { diagnosticPlayer(ctx) }
    val frameProbe = remember { VoiceCenterFrameProbe() }
    DisposableEffect(frameProbe) {
        frameProbe.start()
        onDispose { frameProbe.finish() }
    }
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
    var historyCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var aliases by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var manualReviews by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var hideReviewedSimilar by remember { mutableStateOf(false) }
    var onlyPortuguese by remember { mutableStateOf(false) }
    var sampleMode by remember { mutableStateOf(GeminiVoiceSampleText.STANDARD) }
    var customText by remember { mutableStateOf("") }
    var sampleDraftMode by remember { mutableStateOf(GeminiVoiceSampleText.STANDARD) }
    var sampleDraftText by remember { mutableStateOf("") }
    var sampleLanguage by remember { mutableStateOf(GeminiSpeechLanguage.PORTUGUESE_BR) }
    var sampleDraftLanguage by remember { mutableStateOf(GeminiSpeechLanguage.PORTUGUESE_BR) }
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
    var onlyWithHistory by remember { mutableStateOf(false) }
    var sampleModel by remember { mutableStateOf(initialModel) }
    var modelMenu by remember { mutableStateOf(false) }
    var storageMenu by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
    var compareOpen by remember { mutableStateOf(false) }
    var compareA by remember { mutableStateOf<String?>(null) }
    var compareB by remember { mutableStateOf<String?>(null) }
    var blindComparison by remember { mutableStateOf(false) }
    var blindRevealed by remember { mutableStateOf(false) }
    var lastBlindPair by remember { mutableStateOf<String?>(null) }
    var detailsVoice by remember { mutableStateOf<GeminiVoice?>(null) }
    var historyVoice by remember { mutableStateOf<GeminiVoice?>(null) }
    var historyItems by remember { mutableStateOf<List<GeminiSavedVoicePreview>>(emptyList()) }
    var pendingHistoryDelete by remember { mutableStateOf<GeminiSavedVoicePreview?>(null) }
    var pendingHistoryExport by rememberSaveable { mutableStateOf<String?>(null) }
    var editVoice by remember { mutableStateOf<GeminiVoice?>(null) }
    var aliasDraft by remember { mutableStateOf("") }
    var pendingSample by remember { mutableStateOf<GeminiVoice?>(null) }
    var pendingDelete by remember { mutableStateOf(false) }
    // A paginação é local: as 500 vozes continuam pesquisáveis, sem chamadas adicionais à API.
    var page by rememberSaveable { mutableIntStateOf(0) }
    var debouncedQuery by remember { mutableStateOf("") }
    var pageRequestedAtMs by remember { mutableLongStateOf(0L) }
    val voiceListState = rememberLazyListState()
    val openingAtMs = remember { SystemClock.elapsedRealtime() }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(180L)
        debouncedQuery = query
    }


    val wavPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        val hash = pendingHistoryExport
        pendingHistoryExport = null
        if (uri != null && hash != null) scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val source = cache.find(hash) ?: throw IOException("A prévia não está disponível.")
                    ctx.contentResolver.openOutputStream(uri, "w")?.use { output ->
                        source.inputStream().use { input -> input.copyTo(output) }
                    } ?: throw IOException("Não foi possível escrever o WAV.")
                }
                status = "Prévia exportada: ${bytes / 1024} KiB. Nenhuma chamada Google foi feita."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                status = "Falha ao salvar WAV no destino escolhido; a prévia original foi preservada."
            }
        }
    }

    fun title(voice: GeminiVoice) = GeminiVoiceAliases.title(voice, aliases)
    fun sampleText(): String = GeminiVoiceSampleText.content(sampleMode, customText)
    fun sampleId(voice: GeminiVoice): String = GeminiPreviewIdentity.idForText(
        "", sampleModel, voice.id, GeminiSpeechLanguage.style(styleInstruction, sampleLanguage), sampleText())

    suspend fun updatePreviewIndicators() {
        val startedAtMs = SystemClock.elapsedRealtime()
        val keys = voices.map { it.id to sampleId(it) }
        val state = withContext(Dispatchers.IO) {
            Triple(keys.mapNotNull { (id, hash) -> id.takeIf { cache.exists(hash) } }.toSet(),
                cache.usageBytes(), cache.countsByVoice())
        }
        previewIds = state.first
        occupiedBytes = state.second
        historyCounts = state.third
        DiagnosticRecorder.voiceCenterPerformance("cache", SystemClock.elapsedRealtime() - startedAtMs, keys.size)
    }
    suspend fun fetchAllLanguages() {
        if (working) return
        working = true
        try {
            val key = withContext(Dispatchers.IO) { credentials.read() }
                ?: throw IllegalStateException("Configure uma chave Gemini em Serviços e APIs.")
            val fetched = client.voicesAllLanguages(key)
            if (fetched.voices.isEmpty()) {
                status = "O Google não retornou outras vozes. Seu catálogo salvo foi preservado."
            } else {
                val oldIds = voices.map { it.id }.toSet()
                val saved = withContext(Dispatchers.IO) {
                    val merged = GeminiVoiceDiscovery.merge(voices, fetched.voices)
                    val snapshot = library.save(merged)
                    favorites.saveNames(snapshot.voices.associate { it.id to GeminiVoicePresentation.label(it) })
                    snapshot
                }
                voices = saved.voices
                updatedAt = saved.updatedAt
                onlyPortuguese = false
                updatePreviewIndicators()
                status = "${saved.voices.count { it.id !in oldIds }} vozes adicionais salvas; " +
                    "${saved.voices.size} no catálogo local (limite ${GeminiVoiceLibrary.MAX_VOICES}). " +
                    "Idioma informado não garante pronúncia correta em português."
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            status = "Consulta de outros idiomas falhou: ${GeminiVoiceCatalog.safeMessage(e)} " +
                "Nenhuma voz salva foi apagada."
        } finally { working = false }
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
                    val snapshot = library.save(GeminiVoiceDiscovery.merge(voices, fetched.voices))
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
            Pair(aliasStore.load(), Triple(samplePrefs.mode(), samplePrefs.custom(), samplePrefs.language()))
        }
        val storedReviews = withContext(Dispatchers.IO) { reviewStore.load() }
        voices = snapshot.first.voices
        updatedAt = snapshot.first.updatedAt
        favoriteIds = snapshot.second
        autoRefresh = snapshot.third.first
        storageLimit = snapshot.third.second
        occupiedBytes = snapshot.third.third
        aliases = personal.first
        manualReviews = storedReviews
        sampleMode = personal.second.first
        customText = personal.second.second
        sampleLanguage = personal.second.third
        sampleDraftMode = personal.second.first
        sampleDraftText = personal.second.second
        sampleDraftLanguage = personal.second.third
        loaded = true
        updatePreviewIndicators()
        DiagnosticRecorder.voiceCenterPerformance("open", SystemClock.elapsedRealtime() - openingAtMs, voices.size)
    }
    LaunchedEffect(historyVoice) {
        historyItems = historyVoice?.let { selected ->
            withContext(Dispatchers.IO) { cache.listByVoice(selected.id) }
        }.orEmpty()
    }
    suspend fun reloadHistory() {
        updatePreviewIndicators()
        historyItems = historyVoice?.let { selected ->
            withContext(Dispatchers.IO) { cache.listByVoice(selected.id) }
        }.orEmpty()
    }
    // Atualização automática é opt-in; o padrão é funcionar somente com snapshot local.
    LaunchedEffect(loaded, autoRefresh) {
        if (loaded && autoRefresh) fetchFromGoogle()
    }
    LaunchedEffect(sampleModel, styleInstruction, voices, sampleMode, customText, sampleLanguage) {
        if (loaded) updatePreviewIndicators()
    }
    LaunchedEffect(debouncedQuery, gender, pitch, accent, contextFilter, persona, language,
        onlyFavorites, onlySaved, onlyWithHistory, onlyPortuguese, hideReviewedSimilar) {
        page = 0
    }
    // Indexa uma vez os textos normalizados; não recalcula 500 strings para cada recomposição.
    val searchIndex = remember(voices, aliases) {
        voices.associate { voice -> voice.id to GeminiVoicePresentation.fold(
            GeminiVoicePresentation.searchableText(voice) + " " + aliases[voice.id].orEmpty()) }
    }
    val reviewedHidden = remember(voices, manualReviews, favoriteIds, previewIds, hideReviewedSimilar) {
        if (hideReviewedSimilar) GeminiVoiceDiscovery.hiddenFromReview(
            voices, manualReviews, favoriteIds, previewIds) else emptySet()
    }
    val filteredResult = remember(voices, debouncedQuery, searchIndex, gender, pitch, accent,
        contextFilter, persona, language, onlyFavorites, onlySaved, onlyWithHistory,
        onlyPortuguese, favoriteIds, previewIds, historyCounts, reviewedHidden, aliases) {
        val startNs = SystemClock.elapsedRealtimeNanos()
        val target = GeminiVoicePresentation.fold(debouncedQuery.trim())
        val matching = GeminiVoiceSearch.byGender(voices.filter { voice ->
            target.isEmpty() || searchIndex[voice.id]?.contains(target) == true
        }, gender)
        val result = GeminiVoiceSearch.byPitch(matching, pitch)
            .let { GeminiVoiceSearch.byMetadata(it, "accent", accent) }
            .let { GeminiVoiceSearch.byMetadata(it, "context", contextFilter) }
            .let { GeminiVoiceSearch.byMetadata(it, "persona", persona) }
            .let { GeminiVoiceSearch.byMetadata(it, "language", language) }
            .filter { (!onlyFavorites || it.id in favoriteIds) && (!onlySaved || it.id in previewIds) &&
                (!onlyWithHistory || historyCounts.getOrDefault(it.id, 0) > 0) &&
                (!onlyPortuguese || GeminiVoiceDiscovery.isPortuguese(it)) && it.id !in reviewedHidden }
            .sortedWith(compareByDescending<GeminiVoice> { it.id in favoriteIds }
                .thenBy { GeminiVoiceAliases.title(it, aliases) })
        VoiceCenterFilterResult(result, (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000L)
    }
    val displayed = filteredResult.voices
    LaunchedEffect(filteredResult) {
        DiagnosticRecorder.voiceCenterPerformance("filter", filteredResult.elapsedMs, displayed.size)
    }
    val totalPages = GeminiVoicePaging.totalPages(displayed.size)
    val currentPage = page.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val visible = GeminiVoicePaging.slice(displayed, currentPage)
    val pageRange = GeminiVoicePaging.range(displayed.size, currentPage)
    LaunchedEffect(currentPage, displayed) {
        voiceListState.scrollToItem(0)
        if (pageRequestedAtMs > 0L) {
            withFrameNanos { }
            DiagnosticRecorder.voiceCenterPerformance("page", SystemClock.elapsedRealtime() - pageRequestedAtMs, visible.size)
            pageRequestedAtMs = 0L
        }
    }
    val offlineCandidates = remember(displayed, previewIds) {
        displayed.asSequence().map { it.id }.filter { it in previewIds }.toList()
    }
    val chosenA = voices.firstOrNull { it.id == compareA }
    val chosenB = voices.firstOrNull { it.id == compareB }
    val comparisonKey = compareA?.let { a -> compareB?.let { b -> GeminiVoiceDiscovery.pairKey(a, b) } }
    val currentReview = comparisonKey?.let { manualReviews[it] }
    val advancedCount = listOf(accent, contextFilter, persona, language).count { it.isNotBlank() } +
        (if (gender != "Todas") 1 else 0) + (if (pitch != "Todos") 1 else 0) +
        (if (onlySaved) 1 else 0) + (if (onlyWithHistory) 1 else 0) + (if (onlyPortuguese) 1 else 0) +
        (if (hideReviewedSimilar) 1 else 0)

    fun startBlindComparison() {
        val pair = GeminiVoiceDiscovery.nextOfflinePair(
            offlineCandidates.shuffled(), previewIds, manualReviews, lastBlindPair)
        if (pair == null) {
            status = "Salve prévias de duas vozes com a mesma frase, modelo e estilo para comparar sem gastar API."
            return
        }
        compareA = pair.first
        compareB = pair.second
        lastBlindPair = GeminiVoiceDiscovery.pairKey(pair.first, pair.second)
        blindComparison = true
        blindRevealed = false
        compareOpen = true
        status = "Duas prévias locais prontas. Ouça 1 e 2; os nomes ficam ocultos até avaliar ou revelar."
    }

    fun recordReview(value: String?) {
        val a = compareA ?: return
        val b = compareB ?: return
        scope.launch {
            val saved = withContext(Dispatchers.IO) { reviewStore.save(a, b, value) }
            if (saved == null) status = "Não foi possível salvar a comparação local."
            else {
                manualReviews = saved
                if (blindComparison && value != null) blindRevealed = true
                status = if (value == null) "Comparação removida." else
                    "Avaliação salva apenas neste aparelho; não é diagnóstico acústico."
            }
        }
    }

    fun listen(voice: GeminiVoice, blindLabel: String? = null) {
        scope.launch {
            working = true
            try {
                // Voz e modelo/tipo de prévia devem corresponder exatamente ao cache.
                val local = withContext(Dispatchers.IO) { cache.find(sampleId(voice)) }
                if (local != null) {
                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(local)))
                    player.prepare(); player.play()
                    status = if (blindLabel != null) "Reproduzindo $blindLabel sem chamada Gemini."
                        else "Reproduzindo ${title(voice)} sem chamada Gemini."
                } else if (blindLabel != null) {
                    status = "Prévia offline indisponível. Reconfigure o teste; nada foi enviado ao Gemini."
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
            IconButton(onClick = { sampleDraftMode = sampleMode; sampleDraftText = customText;
                sampleDraftLanguage = sampleLanguage; settingsOpen = true }) {
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
        // Filtros mais utilizados ficam na tela; os filtros detalhados continuam no diálogo.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !onlyFavorites && !onlyPortuguese && !onlyWithHistory && gender == "Todas" &&
                    pitch == "Todos" && accent.isBlank() && contextFilter.isBlank() &&
                    persona.isBlank() && language.isBlank() && !onlySaved && !hideReviewedSimilar,
                onClick = {
                    onlyFavorites = false; onlyPortuguese = false; onlyWithHistory = false
                    gender = "Todas"; pitch = "Todos"; accent = ""; contextFilter = ""
                    persona = ""; language = ""; onlySaved = false; hideReviewedSimilar = false
                }, label = { Text("Todas") })
            FilterChip(selected = onlyFavorites, onClick = { onlyFavorites = !onlyFavorites },
                label = { Text("★ Favoritas (${favoriteIds.size})") })
            FilterChip(selected = onlyPortuguese, onClick = { onlyPortuguese = !onlyPortuguese },
                label = { Text("Português BR") })
            FilterChip(selected = gender == "Masculinas", onClick = {
                gender = if (gender == "Masculinas") "Todas" else "Masculinas"
            }, label = { Text("Masculinas") })
            FilterChip(selected = gender == "Femininas", onClick = {
                gender = if (gender == "Femininas") "Todas" else "Femininas"
            }, label = { Text("Femininas") })
            FilterChip(selected = onlyWithHistory, onClick = { onlyWithHistory = !onlyWithHistory },
                label = { Text("Com prévia") })
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { filtersOpen = true }, shape = UaiShapes.button) {
                Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (advancedCount == 0) "Filtros" else "Filtros ($advancedCount)")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                compareOpen = !compareOpen
                if (!compareOpen) { blindComparison = false; blindRevealed = false }
            }) {
                Icon(Icons.Default.CompareArrows, null, Modifier.size(18.dp))
                Text("A/B")
            }
        }
        if (compareOpen) {
            Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (blindComparison && !blindRevealed) "Escuta sem nomes"
                            else "Comparar timbres", fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { startBlindComparison() },
                            enabled = !working && offlineCandidates.size >= 2) {
                            Text(if (blindComparison) "Outro par" else "Teste cego", fontSize = 12.sp)
                        }
                    }
                    if (offlineCandidates.size < 2) Text(
                        "Teste cego: requer duas prévias salvas com esta frase, modelo e estilo.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Use a mesma frase e modelo. Se a prévia não existir, o app pedirá confirmação.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompareVoiceButton(if (blindComparison && !blindRevealed) "1" else "A",
                            if (blindComparison && !blindRevealed) "Voz oculta" else chosenA?.let(::title),
                            !working, Modifier.weight(1f)) {
                            chosenA?.let { listen(it, if (blindComparison && !blindRevealed) "prévia 1" else null) }
                                ?: run { status = "Escolha a voz A nos cartões abaixo." }
                        }
                        CompareVoiceButton(if (blindComparison && !blindRevealed) "2" else "B",
                            if (blindComparison && !blindRevealed) "Voz oculta" else chosenB?.let(::title),
                            !working, Modifier.weight(1f)) {
                            chosenB?.let { listen(it, if (blindComparison && !blindRevealed) "prévia 2" else null) }
                                ?: run { status = "Escolha a voz B nos cartões abaixo." }
                        }
                    }
                    if (blindComparison && !blindRevealed) {
                        Text("Sem nomes para reduzir a influência da escolha. Sua avaliação será revelada ao salvar.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { blindRevealed = true }) { Text("Revelar nomes sem avaliar") }
                    }
                    if (comparisonKey != null) {
                        Text("Sua avaliação: " + when (currentReview) {
                            GeminiVoiceDiscovery.SIMILAR -> "Parecidas"
                            GeminiVoiceDiscovery.DIFFERENT -> "Diferentes"
                            else -> "Ainda não avaliadas"
                        }, fontSize = 11.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected = currentReview == GeminiVoiceDiscovery.SIMILAR,
                                onClick = { recordReview(GeminiVoiceDiscovery.SIMILAR) },
                                label = { Text("Parecidas") })
                            FilterChip(selected = currentReview == GeminiVoiceDiscovery.DIFFERENT,
                                onClick = { recordReview(GeminiVoiceDiscovery.DIFFERENT) },
                                label = { Text("Diferentes") })
                            TextButton(onClick = { recordReview(null) }, enabled = currentReview != null) {
                                Text("Limpar")
                            }
                        }
                        Text("Avaliação subjetiva; não identifica pessoas ou vozes iguais.",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else Text("Toque em A ou B nos cartões para escolher. Não gera áudio automaticamente.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${displayed.size} de ${voices.size} vozes · ${pageRange.first}–${pageRange.second}", fontSize = 12.sp,
                modifier = Modifier.weight(1f))
            if (hideReviewedSimilar) Text("Sem similares · ", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary)
            if (onlySaved) Text("Com prévia · ", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary)
            Text("${occupiedBytes / (1024 * 1024)} MB em prévias", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (totalPages > 1 && loaded) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = {
                    pageRequestedAtMs = SystemClock.elapsedRealtime()
                    page = (currentPage - 1).coerceAtLeast(0)
                }, enabled = currentPage > 0) {
                    Icon(Icons.Default.ChevronLeft, "Página anterior")
                }
                Text("Página ${currentPage + 1} de $totalPages", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = {
                    pageRequestedAtMs = SystemClock.elapsedRealtime()
                    page = (currentPage + 1).coerceAtMost(totalPages - 1)
                }, enabled = currentPage < totalPages - 1) {
                    Icon(Icons.Default.ChevronRight, "Próxima página")
                }
            }
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
            LazyColumn(Modifier.weight(1f), state = voiceListState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(bottom = 14.dp)) {
                items(visible, key = { it.id }) { voice ->
                    val saved = voice.id in previewIds
                    Surface(onClick = { onChoose?.invoke(voice) }, enabled = onChoose != null,
                        modifier = Modifier.fillMaxWidth(), shape = UaiShapes.card,
                        color = if (voice.id == selectedVoiceId) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    val historyCount = historyCounts[voice.id] ?: 0
                                    if (saved) Text("Prévia desta frase/modelo", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary)
                                    if (historyCount > 0) Text("$historyCount prévia(s) no histórico local",
                                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (voice.id == selectedVoiceId) Icon(Icons.Default.CheckCircle,
                                    "Selecionada", Modifier.size(17.dp))
                            }
                            // As quatro ações ficam na MESMA linha, sem disputar a largura do nome.
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    aliasDraft = aliases[voice.id].orEmpty()
                                    editVoice = voice
                                }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Edit, "Renomear ${title(voice)}", Modifier.size(20.dp))
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
                                if (compareOpen && !(blindComparison && !blindRevealed)) {
                                    TextButton(onClick = {
                                        blindComparison = false; compareA = voice.id
                                        if (compareB == voice.id) compareB = null
                                    }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(36.dp)) {
                                        Text(if (compareA == voice.id) "A ✓" else "A", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = {
                                        blindComparison = false; compareB = voice.id
                                        if (compareA == voice.id) compareA = null
                                    }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(36.dp)) {
                                        Text(if (compareB == voice.id) "B ✓" else "B", fontSize = 12.sp)
                                    }
                                }
                                IconButton(onClick = { detailsVoice = voice }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.MoreVert, "Detalhes e comparação")
                                }
                            }
                        }
                    }
                }
                if (totalPages > 1) {
                    item(key = "voice_page_controls") {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = {
                                pageRequestedAtMs = SystemClock.elapsedRealtime()
                                page = (currentPage - 1).coerceAtLeast(0)
                            }, enabled = currentPage > 0, shape = UaiShapes.button) {
                                Icon(Icons.Default.ChevronLeft, null, Modifier.size(18.dp))
                                Text("Anterior")
                            }
                            Text("${currentPage + 1} de $totalPages", fontSize = 12.sp)
                            OutlinedButton(onClick = {
                                pageRequestedAtMs = SystemClock.elapsedRealtime()
                                page = (currentPage + 1).coerceAtMost(totalPages - 1)
                            }, enabled = currentPage < totalPages - 1, shape = UaiShapes.button) {
                                Text("Próxima")
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                            }
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
                    Checkbox(checked = onlyPortuguese, onCheckedChange = { onlyPortuguese = it })
                    Text("Somente vozes catalogadas como português BR")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hideReviewedSimilar, onCheckedChange = { hideReviewedSimilar = it })
                    Text("Ocultar vozes que marquei como parecidas")
                }
                Text("${manualReviews.values.count { it == GeminiVoiceDiscovery.SIMILAR }} pares marcados parecidos. " +
                    "A classificação é sua e permanece apenas neste aparelho.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlySaved, onCheckedChange = { onlySaved = it })
                    Text("Somente prévias da frase/modelo atuais")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlyWithHistory, onCheckedChange = { onlyWithHistory = it })
                    Text("Vozes com qualquer prévia no histórico offline")
                }
                Text("Sem inferir sotaque, idade ou identidade pela gravação.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { filtersOpen = false }) { Text("Ver resultados") } },
        dismissButton = { TextButton(onClick = {
            gender = "Todas"; pitch = "Todos"; accent = ""; persona = ""; contextFilter = ""
            language = ""; onlySaved = false; onlyWithHistory = false; onlyPortuguese = false; hideReviewedSimilar = false
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
                HorizontalDivider()
                Text("Descobrir vozes de outros idiomas", fontWeight = FontWeight.Medium)
                Text("Consulta manual do catálogo geral do Google. Mantém vozes já salvas, " +
                    "com limite de ${GeminiVoiceLibrary.MAX_VOICES} registros. Uma voz inglesa pode " +
                    "não pronunciar português corretamente. Não gera áudio.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = {
                    settingsOpen = false
                    scope.launch { fetchAllLanguages() }
                }, enabled = !working, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                    Icon(Icons.Default.Language, null)
                    Text("Consultar todos os idiomas")
                }
                HorizontalDivider()
                Text("Idioma da prévia", fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeminiSpeechLanguage.choices.forEach { choice ->
                        FilterChip(selected = sampleDraftLanguage == choice,
                            onClick = { sampleDraftLanguage = choice },
                            label = { Text(GeminiSpeechLanguage.label(choice)) })
                    }
                }
                Text("Português (Brasil) orienta a fala, inclusive de vozes de outros idiomas; " +
                    "o sotaque e a pronúncia podem variar. Gerar nova prévia pode consumir cota.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
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
            } else if (samplePrefs.save(sampleDraftMode, sampleDraftText, sampleDraftLanguage)) {
                sampleMode = sampleDraftMode; customText = sampleDraftText; sampleLanguage = sampleDraftLanguage
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
                    if (!GeminiVoiceDiscovery.isPortuguese(voice)) Text(
                        "Essa voz não está catalogada como pt-BR. O suporte e a pronúncia em português " +
                        "precisam ser verificados por uma prévia real.", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ID: ${voice.id}", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {
                        detailsVoice = null; aliasDraft = aliases[voice.id].orEmpty(); editVoice = voice
                    }, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                        Icon(Icons.Default.Edit, null); Text("Renomear voz")
                    }
                    OutlinedButton(onClick = {
                        detailsVoice = null; historyVoice = voice
                    }, modifier = Modifier.fillMaxWidth(), enabled = (historyCounts[voice.id] ?: 0) > 0,
                        shape = UaiShapes.button) {
                        Icon(Icons.Default.FolderOpen, null)
                        Text("Prévias offline (${historyCounts[voice.id] ?: 0})")
                    }
                    Text("Amostras geradas com outras frases e modelos continuam acessíveis. " +
                        "Ouvir e exportar não chama o Gemini.", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            blindComparison = false; compareA = voice.id
                            if (compareB == voice.id) compareB = null
                            compareOpen = true; detailsVoice = null
                        },
                            modifier = Modifier.weight(1f), shape = UaiShapes.button) { Text("Escolher A") }
                        OutlinedButton(onClick = {
                            blindComparison = false; compareB = voice.id
                            if (compareA == voice.id) compareA = null
                            compareOpen = true; detailsVoice = null
                        },
                            modifier = Modifier.weight(1f), shape = UaiShapes.button) { Text("Escolher B") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { detailsVoice = null }) { Text("Fechar") } })
    }
    historyVoice?.let { voice ->
        AlertDialog(onDismissRequest = { historyVoice = null },
            title = { Text("Prévias de ${title(voice)}") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Arquivos WAV já gerados neste aparelho. Sem novas cobranças ou " +
                        "requisições ao Google. O áudio não substitui uma fala do projeto.", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (historyItems.isEmpty()) Text("Nenhuma prévia disponível para esta voz.")
                    historyItems.forEachIndexed { index, item ->
                        Surface(shape = UaiShapes.card,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)) {
                            Column(Modifier.fillMaxWidth().padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Amostra ${index + 1} · último uso: " +
                                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                        .format(Date(item.savedAt)), fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp)
                                Text("WAV · ${item.bytes / 1024} KiB · frase/modelo anteriores podem variar",
                                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(onClick = {
                                        scope.launch {
                                            try {
                                                val audio = withContext(Dispatchers.IO) { cache.find(item.key) }
                                                    ?: throw IOException("Prévia indisponível")
                                                player.setMediaItem(MediaItem.fromUri(Uri.fromFile(audio)))
                                                player.prepare(); player.play()
                                                status = "Reproduzindo prévia local. Nenhuma chamada Google."
                                            } catch (e: CancellationException) { throw e }
                                            catch (_: Exception) {
                                                status = "Não foi possível reproduzir a prévia salva."
                                                reloadHistory()
                                            }
                                        }
                                    }) { Icon(Icons.Default.PlayArrow, "Ouvir prévia") }
                                    IconButton(onClick = { player.stop() }) {
                                        Icon(Icons.Default.Stop, "Parar reprodução")
                                    }
                                    IconButton(onClick = {
                                        pendingHistoryExport = item.key
                                        wavPicker.launch("UaiCortes-previa-${index + 1}.wav")
                                    }) { Icon(Icons.Default.SaveAlt, "Salvar WAV fora do aplicativo") }
                                    IconButton(onClick = { pendingHistoryDelete = item }) {
                                        Icon(Icons.Default.DeleteOutline, "Excluir somente esta prévia")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyVoice = null }) { Text("Fechar") } })
    }
    pendingHistoryDelete?.let { preview ->
        AlertDialog(onDismissRequest = { pendingHistoryDelete = null },
            title = { Text("Excluir apenas esta prévia?") },
            text = { Text("A exclusão afeta somente este WAV salvo localmente. " +
                "A voz, os favoritos, os áudios dos projetos e as outras prévias serão preservados.") },
            confirmButton = { TextButton(onClick = {
                pendingHistoryDelete = null
                scope.launch {
                    val removed = withContext(Dispatchers.IO) { cache.removeOne(preview.key) }
                    if (removed) {
                        player.stop()
                        reloadHistory()
                        status = "Prévia removida; nenhuma fala de projeto foi alterada."
                    } else status = "A prévia não pôde ser removida."
                }
            }) { Text("Excluir WAV") } },
            dismissButton = { TextButton(onClick = { pendingHistoryDelete = null }) { Text("Cancelar") } })
    }
    editVoice?.let { voice ->
        AlertDialog(onDismissRequest = { editVoice = null },
            title = { Text("Renomear voz Google") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Nome original: ${GeminiVoicePresentation.label(voice)}", fontSize = 12.sp)
                    OutlinedTextField(value = aliasDraft,
                        onValueChange = { if (it.length <= GeminiVoiceAliases.MAX_ALIAS_CHARS) aliasDraft = it },
                        label = { Text("Nome personalizado") }, singleLine = true)
                    Text("O nome fica salvo apenas neste aparelho. Ele não muda a voz, " +
                        "a pronúncia ou o ID usado pela API Google.", fontSize = 11.sp)
                    if (voice.id in aliases) TextButton(onClick = { aliasDraft = "" }) {
                        Text("Restaurar nome original")
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { aliasStore.save(voice.id, aliasDraft) }
                    if (saved != null) {
                        aliases = saved; editVoice = null
                        status = if (aliasDraft.isBlank()) "Nome original restaurado." else "Nome da voz salvo."
                    } else status = "Não foi possível salvar o nome da voz."
                }
            }, enabled = aliasDraft.isBlank() || GeminiVoiceAliases.validAlias(aliasDraft.trim())) {
                Text("Salvar nome")
            } },
            dismissButton = { TextButton(onClick = { editVoice = null }) { Text("Cancelar") } })
    }
    pendingSample?.let { voice ->
        AlertDialog(onDismissRequest = { pendingSample = null },
            title = { Text("Gerar prévia de ${title(voice)}?") },
            text = { Text("Será gerada uma amostra com a frase escolhida. Isso envia texto ao Google " +
                "e pode consumir cota ou gerar cobrança. Depois a reprodução será local até você apagar o arquivo. " +
                "Não será inserida no vídeo." +
                (if (GeminiVoiceDiscovery.isPortuguese(voice)) "" else
                    " A narração tentará português brasileiro, mas o sotaque pode variar.")) },
            confirmButton = { TextButton(onClick = {
                pendingSample = null
                scope.launch {
                    working = true
                    try {
                        // Fixar configuração ANTES da primeira suspensão: trocar modelo/frase na tela
                        // não pode salvar um WAV de outro perfil sob a chave da configuração antiga.
                        val requestedModel = sampleModel
                        val requestedText = sampleText()
                        val requestedStyle = styleInstruction
                        val requestedLanguage = sampleLanguage
                        val id = GeminiPreviewIdentity.idForText("", requestedModel,
                            voice.id, GeminiSpeechLanguage.style(requestedStyle, requestedLanguage), requestedText)
                        val existing = withContext(Dispatchers.IO) { cache.find(id) }
                        val file = if (existing != null) existing else {
                            val key = withContext(Dispatchers.IO) { credentials.read() }
                                ?: error("Chave Gemini ausente.")
                            withContext(Dispatchers.IO) { pacing.beforeRequest(requestedModel) { } }
                            val wave = client.synthesize(key, requestedModel, voice.id,
                                requestedStyle, requestedText, requestedLanguage)
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
