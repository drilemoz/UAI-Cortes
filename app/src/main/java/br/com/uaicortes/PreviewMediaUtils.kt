package br.com.uaicortes

import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

/** Calcula proporção sem carregar um bitmap integral em memória. */
internal fun mediaAspect(context: android.content.Context, uri: String, isImage: Boolean): Float {
    return runCatching {
        if (isImage) {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0)
                options.outWidth.toFloat() / options.outHeight else 16f / 9f
        } else {
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(context, Uri.parse(uri))
                val w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 16f
                val h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 9f
                val rotation = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (rotation % 180 != 0) h / w.coerceAtLeast(1f) else w / h.coerceAtLeast(1f)
            } finally { r.release() }
        }
    }.getOrDefault(16f / 9f).coerceIn(0.1f, 10f)
}

internal fun projectPosition(player: ExoPlayer, clips: List<Clip>, total: Long): Long =
    TimelinePlaybackCoordinator.projectPosition(
        clips.map { it.durationMs }, player.currentMediaItemIndex, player.currentPosition
    ).coerceIn(0L, total.coerceAtLeast(0L))

internal fun seekProjectPosition(player: ExoPlayer, clips: List<Clip>, position: Long) {
    val dest = TimelinePlaybackCoordinator.target(clips.map { it.durationMs }, position) ?: return
    player.seekTo(dest.clipIndex, dest.localMs)
}

/**
 * Centraliza as chamadas Media3 @UnstableApi fora das lambdas AndroidView do Compose.
 * Android Lint não considera o opt-in do composable externo suficiente em certas lambdas.
 * A prévia permanece em TextureView para compor PiPs e guias por cima do vídeo.
 */
@OptIn(UnstableApi::class)
internal fun previewPlayer(
    context: android.content.Context,
    transparentShutter: Boolean = false,
): PlayerView =
    (android.view.LayoutInflater.from(context)
        .inflate(R.layout.player_preview_texture, null, false) as PlayerView).apply {
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        if (transparentShutter) setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
    }
