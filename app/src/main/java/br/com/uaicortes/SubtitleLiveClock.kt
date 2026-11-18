package br.com.uaicortes

import androidx.compose.runtime.*
import androidx.media3.common.Player

/** Reads the same media position as the preview player, without speeding up
 * recomposition of the main timeline or altering the exporter clock. */
@Composable
internal fun rememberSubtitleLiveClock(player: Player, clips: List<Clip>, totalMs: Long,
    fallbackMs: Long, active: Boolean, route: String): State<Long> {
    val durations = remember(clips) { clips.map { it.durationMs } }
    val fallback by rememberUpdatedState(fallbackMs)
    return produceState(fallbackMs, player, durations, totalMs, active, route) {
        value = fallback
        if (active && durations.isNotEmpty()) {
            var maxClockDelta = 0L
            var frames = 0L
            try {
                while (true) withFrameNanos {
                    val position = TimelinePlaybackCoordinator.projectPosition(
                        durations, player.currentMediaItemIndex, player.currentPosition
                    ).coerceIn(0L, totalMs.coerceAtLeast(0L))
                    maxClockDelta = maxOf(maxClockDelta, kotlin.math.abs(position - fallback))
                    frames++
                    value = position
                }
            } finally {
                if (frames > 0L) DiagnosticRecorder.previewClock(route, maxClockDelta)
            }
        }
    }
}

@Composable
internal fun LiveSubtitleLayers(cues: List<SubtitleCue>, clock: State<Long>,
    fallbackMs: Long, playing: Boolean, selectedId: String? = null,
    onPositionCommit: (Float, Float) -> Unit = { _, _ -> }) {
    val playbackMs = if (playing) clock.value else fallbackMs
    SubtitlePreviewPolicy.firstAt(cues, playbackMs)?.let { cue ->
        key(cue.id) {
            SubtitleOverlayOnFrame(cue, editable = !playing && cue.id == selectedId,
                playbackMs = playbackMs, onPositionCommit = onPositionCommit)
        }
    }
}
