package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Acesso aos serviços usados pelo editor. Abrir esta tela não envia áudio ou vídeo. */
@Composable
internal fun VoiceServicesScreen(
    back: () -> Unit,
    openGemini: () -> Unit,
    openPolly: () -> Unit,
    openElevenLabs: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("IA e voz", back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemCard(Icons.Default.AutoAwesome, "Google Gemini",
                "Chaves, modelos, legendas e narrações", onClick = openGemini)
            ItemCard(Icons.Default.RecordVoiceOver, "Amazon Polly",
                "Chaves AWS, vozes e teste de narração", onClick = openPolly)
            ItemCard(Icons.Default.GraphicEq, "ElevenLabs",
                "Chave, modelos e vozes", onClick = openElevenLabs)
        }
    }
}
