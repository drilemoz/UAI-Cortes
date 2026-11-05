package br.com.uaicortes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.UUID

/** Wizard: um objetivo por tela, com parâmetros avançados recolhidos. */
internal val studioWizardTitles = listOf("Estilo", "Personagens", "Cenários", "Cenas", "Flow")

@Composable
internal fun StudioWizardStepHeader(step: Int, onNavigate: (Int) -> Unit) {
    Text("ETAPA ${step + 1} DE 5", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary)
    LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        studioWizardTitles.forEachIndexed { index, title ->
            FilterChip(selected = index == step, onClick = { onNavigate(index) },
                label = { Text("${index + 1}. $title", maxLines = 1) })
        }
    }
}

@Composable
internal fun StudioWizardStyle(story: StudioStory, savedStyles: List<String>,
    onChange: (StudioStory) -> Unit, onSaveStyle: (String) -> Unit) {
    var customOpen by remember { mutableStateOf(false) }
    var ideaOpen by remember { mutableStateOf(story.idea.isNotBlank()) }
    var showPreferences by remember { mutableStateOf(false) }
    var originalOpen by remember { mutableStateOf(false) }
    Text("Qual será o visual dos vídeos?", style = MaterialTheme.typography.headlineSmall)
    Text("Escolha uma vez. Personagens e cenários seguirão este estilo.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val styleCards = listOf(
        Triple("Animação 3D", "🎬", "Personagens com volume e profundidade"),
        Triple("Animação 2D", "🎨", "Desenhos e traços ilustrados"),
        Triple("Realista", "📷", "Aparência cinematográfica"),
        Triple("Personalizado", "✨", "Definir minha própria estética")
    )
    styleCards.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (name, symbol, detail) ->
                val selected = if (name == "Personalizado") story.style !in listOf("Animação 3D", "Animação 2D", "Realista")
                    else name == story.style
                Card(onClick = {
                    if (name == "Personalizado") { customOpen = true; onChange(story.copy(style = "Personalizado")) }
                    else { customOpen = false; onChange(story.copy(style = name)) }
                }, modifier = Modifier.weight(1f).heightIn(min = 136.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selected)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(symbol, style = MaterialTheme.typography.headlineMedium)
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Text(detail, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
    if (customOpen || story.style !in listOf("Animação 3D", "Animação 2D", "Realista")) {
        WizardInput("Descrição do estilo", story.style, 120) { onChange(story.copy(style = it)) }
    }
    if (story.style.isNotBlank() && savedStyles.none { it == story.style }) {
        TextButton(onClick = { onSaveStyle(story.style) }) {
            Icon(Icons.Default.BookmarkAdd, null); Text(" Salvar este estilo para outras histórias")
        }
    }
    if (savedStyles.isNotEmpty()) {
        Text("Estilos salvos", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            savedStyles.forEach { style -> SuggestionChip(onClick = { onChange(story.copy(style = style)); customOpen = false },
                label = { Text(style) }) }
        }
    }
    TextButton(onClick = { ideaOpen = !ideaOpen }) {
        Icon(Icons.Default.Lightbulb, null)
        Text(if (ideaOpen) " Ocultar ideia da história" else " Tenho uma ideia de história (opcional)")
    }
    if (ideaOpen) WizardInput("Ex.: um alienígena rouba a geladeira", story.idea, 5000, 2) {
        onChange(story.copy(idea = it))
    }
    TextButton(onClick = { originalOpen = !originalOpen }) {
        Icon(Icons.Default.Description, null)
        Text(" Colar roteiro que já tenho (opcional)")
    }
    if (originalOpen) WizardInput("Roteiro existente", story.originalScript, 25000, 3) {
        onChange(story.copy(originalScript = it))
    }
    TextButton(onClick = { showPreferences = !showPreferences }) {
        Icon(Icons.Default.Tune, null)
        Text(" Duração e gênero (opcionais)")
    }
    if (showPreferences) {
        Text("Duração prevista", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 60, 75, 120, 180).forEach { sec ->
                FilterChip(selected = story.durationSeconds == sec, onClick = { onChange(story.copy(durationSeconds = sec)) },
                    label = { Text("${sec}s") })
            }
        }
        Text("Gênero", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Humor", "Comédia absurda", "Terror cômico", "Aventura").forEach { genre ->
                FilterChip(selected = story.genre == genre, onClick = { onChange(story.copy(genre = genre)) },
                    label = { Text(genre) })
            }
        }
    }
}

@Composable
private fun WizardInput(label: String, value: String, limit: Int,
    minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { onChange(it.take(limit)) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        minLines = minLines, maxLines = if (minLines == 1) 3 else 5)
}

@Composable
internal fun StudioWizardPicture(imageAsset: String, library: StudioReferenceLibrary, revision: Int,
    modifier: Modifier = Modifier.size(64.dp)) {
    val file = library.asset(imageAsset)
    val bitmap = remember(file?.absolutePath, revision) {
        file?.let { image -> runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(image.path, bounds)
            val largest = maxOf(bounds.outHeight, bounds.outWidth)
            var sample = 1
            while (largest / sample > 512) sample *= 2
            BitmapFactory.decodeFile(image.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }.getOrNull() }
    }
    if (bitmap != null) Image(bitmap, contentDescription = "Referência escolhida da galeria",
        modifier = modifier, contentScale = ContentScale.Crop)
    else Card(modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PersonOutline, contentDescription = "Sem imagem (opcional)")
        }
    }
}

@Composable
internal fun StudioWizardPeople(people: List<StudioPerson>, libraryData: StudioLibraryData?,
    library: StudioReferenceLibrary, revision: Int,
    onUpsert: (StudioPerson) -> Unit, onDelete: (String) -> Unit,
    onImport: (String) -> Unit, onSaveLibrary: (StudioPerson) -> Unit,
    onEnrich: (String) -> Unit, busy: Boolean) {
    var candidate by remember { mutableStateOf<StudioPerson?>(null) }
    var details by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    Text("Quem aparece na história?", style = MaterialTheme.typography.headlineSmall)
    Text("Escolha uma sugestão ou invente alguém. Os detalhes são preenchidos como ponto de partida; pode editar.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Ideias rápidas · sem gastar API", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StudioReferenceSuggestions.characterKinds.forEach { kind ->
            SuggestionChip(label = { Text(kind) }, onClick = { candidate = StudioReferenceSuggestions.person(kind); details = false })
        }
    }
    people.forEachIndexed { index, person ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioWizardPicture(person.imageAsset, library, revision)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("PERSONAGEM ${index + 1}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(person.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(person.appearance.ifBlank { "Sem descrição" }, style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { candidate = person; details = false }, enabled = !busy) {
                    Icon(Icons.Default.Edit, "Editar")
                }
                IconButton(onClick = { deleting = person.id }, enabled = !busy) {
                    Icon(Icons.Default.DeleteOutline, "Excluir")
                }
            }
            Row(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onImport(person.id) }, enabled = !busy) {
                    Icon(Icons.Default.PhotoLibrary, null); Text(" Galeria")
                }
                TextButton(onClick = { onEnrich(person.id) }, enabled = !busy) {
                    Icon(Icons.Default.AutoAwesome, null); Text(" Enriquecer com Gemini")
                }
                TextButton(onClick = { onSaveLibrary(person) }, enabled = !busy && libraryData != null) {
                    Icon(Icons.Default.BookmarkAdd, null); Text(" Salvar na biblioteca")
                }
            }
        }
    }
    if (candidate != null) {
        val person = candidate!!
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (people.any { it.id == person.id }) "Editar personagem" else "Novo personagem",
                    style = MaterialTheme.typography.titleMedium)
                WizardInput("Nome do personagem", person.name, 120) { candidate = person.copy(name = it) }
                WizardInput("Como ele é?", person.appearance, 1500, 2) { candidate = person.copy(appearance = it) }
                TextButton(onClick = { details = !details }) {
                    Icon(if (details) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Text(" Roupa, voz e personalidade (opcional)")
                }
                if (details) {
                    WizardInput("Roupas", person.clothes, 1000) { candidate = person.copy(clothes = it) }
                    WizardInput("Voz", person.voice, 500) { candidate = person.copy(voice = it) }
                    WizardInput("Personalidade", person.personality, 800) { candidate = person.copy(personality = it) }
                    WizardInput("Objetos importantes", person.objects, 800) { candidate = person.copy(objects = it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { candidate = null }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = { onUpsert(person.copy(name = person.name.trim())); candidate = null },
                        enabled = person.name.isNotBlank() && people.size < 30 ||
                            (person.name.isNotBlank() && people.any { it.id == person.id }), modifier = Modifier.weight(1f)) {
                        Text("Salvar personagem")
                    }
                }
            }
        }
    } else if (people.size < 30) {
        Button(onClick = { candidate = StudioPerson() }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
            Icon(Icons.Default.PersonAdd, null); Text(" Criar personagem")
        }
    }
    if (libraryData != null) {
        OutlinedButton(onClick = { libraryOpen = !libraryOpen }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CollectionsBookmark, null)
            Text(" Minha biblioteca (${libraryData.people.size})", Modifier.weight(1f))
            Icon(if (libraryOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        if (libraryOpen) {
            if (libraryData.people.isEmpty()) Text("Salve personagens para reutilizar em outras histórias.",
                style = MaterialTheme.typography.bodySmall)
            libraryData.people.forEach { person ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StudioWizardPicture(person.imageAsset, library, revision, Modifier.size(44.dp))
                    Text(person.name, Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
                    TextButton(onClick = { onUpsert(person.copy(id = UUID.randomUUID().toString())) },
                        enabled = people.size < 30 && !busy) { Text("Usar") }
                }
            }
        }
    } else Text("A biblioteca não pôde ser lida; nenhuma referência compartilhada foi sobrescrita.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null },
        title = { Text("Excluir personagem deste projeto?") },
        text = { Text("A biblioteca e os arquivos da galeria serão preservados.") },
        confirmButton = { TextButton(onClick = { onDelete(id); deleting = null }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
}

@Composable
internal fun StudioWizardPlaces(places: List<StudioPlace>, libraryData: StudioLibraryData?,
    library: StudioReferenceLibrary, revision: Int,
    onUpsert: (StudioPlace) -> Unit, onDelete: (String) -> Unit,
    onImport: (String) -> Unit, onSaveLibrary: (StudioPlace) -> Unit,
    onEnrich: (String) -> Unit, busy: Boolean) {
    var candidate by remember { mutableStateOf<StudioPlace?>(null) }
    var details by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    Text("Onde a história acontece?", style = MaterialTheme.typography.headlineSmall)
    Text("Escolha ambientes para reutilizar nas cenas. É possível continuar sem cadastrar cenários.",
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    Text("Cenários sugeridos", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StudioReferenceSuggestions.places.forEach { kind ->
            SuggestionChip(label = { Text(kind) }, onClick = { candidate = StudioReferenceSuggestions.place(kind); details = false })
        }
    }
    places.forEachIndexed { index, place ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudioWizardPicture(place.imageAsset, library, revision)
                Column(Modifier.weight(1f)) {
                    Text("CENÁRIO ${index + 1}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                    Text(place.details, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { candidate = place; details = false }, enabled = !busy) {
                    Icon(Icons.Default.Edit, "Editar")
                }
                IconButton(onClick = { deleting = place.id }, enabled = !busy) {
                    Icon(Icons.Default.DeleteOutline, "Excluir")
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onImport(place.id) }, enabled = !busy) {
                    Icon(Icons.Default.PhotoLibrary, null); Text(" Galeria")
                }
                TextButton(onClick = { onEnrich(place.id) }, enabled = !busy) {
                    Icon(Icons.Default.AutoAwesome, null); Text(" Enriquecer com Gemini")
                }
                TextButton(onClick = { onSaveLibrary(place) }, enabled = !busy && libraryData != null) {
                    Icon(Icons.Default.BookmarkAdd, null); Text(" Salvar na biblioteca")
                }
            }
        }
    }
    if (candidate != null) {
        val place = candidate!!
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (places.any { it.id == place.id }) "Editar cenário" else "Novo cenário",
                    style = MaterialTheme.typography.titleMedium)
                WizardInput("Nome do cenário", place.name, 120) { candidate = place.copy(name = it) }
                WizardInput("Como é esse lugar?", place.details, 1800, 2) { candidate = place.copy(details = it) }
                TextButton(onClick = { details = !details }) {
                    Icon(if (details) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Text(" Objetos do ambiente (opcional)")
                }
                if (details) WizardInput("Objetos e elementos fixos", place.objects, 900) {
                    candidate = place.copy(objects = it)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { candidate = null }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = { onUpsert(place.copy(name = place.name.trim())); candidate = null },
                        enabled = place.name.isNotBlank() && (places.size < 30 || places.any { it.id == place.id }),
                        modifier = Modifier.weight(1f)) { Text("Salvar cenário") }
                }
            }
        }
    } else if (places.size < 30) {
        Button(onClick = { candidate = StudioPlace() }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
            Icon(Icons.Default.AddLocationAlt, null); Text(" Criar cenário")
        }
    }
    if (libraryData != null) {
        OutlinedButton(onClick = { libraryOpen = !libraryOpen }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CollectionsBookmark, null)
            Text(" Minha biblioteca (${libraryData.places.size})", Modifier.weight(1f))
            Icon(if (libraryOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        if (libraryOpen) {
            if (libraryData.places.isEmpty()) Text("Salve um cenário para usar em outras histórias.",
                style = MaterialTheme.typography.bodySmall)
            libraryData.places.forEach { place ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StudioWizardPicture(place.imageAsset, library, revision, Modifier.size(44.dp))
                    Text(place.name, Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
                    TextButton(onClick = { onUpsert(place.copy(id = UUID.randomUUID().toString())) },
                        enabled = places.size < 30 && !busy) { Text("Usar") }
                }
            }
        }
    } else Text("Biblioteca indisponível: dados existentes preservados.",
        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null },
        title = { Text("Excluir cenário deste projeto?") },
        text = { Text("A biblioteca e as imagens da galeria serão preservadas.") },
        confirmButton = { TextButton(onClick = { onDelete(id); deleting = null }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
}
