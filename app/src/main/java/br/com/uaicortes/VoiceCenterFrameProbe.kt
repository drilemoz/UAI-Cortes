package br.com.uaicortes

import android.os.SystemClock
import android.view.Choreographer

/** Amostra opt-in e curta de intervalos entre callbacks de quadro; NÃO é um detector de ANR.
 * Nunca registra conteúdos, texto, voz, contatos nem caminhos privados.
 */
internal class VoiceCenterFrameProbe {
    private val choreographer = Choreographer.getInstance()
    private var active = false
    private var lastNanos = 0L
    private var startMs = 0L
    private var frames = 0
    private var slowIntervals = 0
    private var longIntervals = 0
    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!active) return
            if (lastNanos > 0L && frameTimeNanos > lastNanos) {
                val gapMs = (frameTimeNanos - lastNanos) / 1_000_000L
                if (gapMs >= 34L) slowIntervals++
                if (gapMs >= 100L) longIntervals++
            }
            lastNanos = frameTimeNanos
            frames++
            if (frames < 90 && SystemClock.elapsedRealtime() - startMs < 2_500L) {
                choreographer.postFrameCallback(this)
            } else finish()
        }
    }

    fun start() {
        if (!DiagnosticRecorder.isEnabled() || active) return
        active = true
        startMs = SystemClock.elapsedRealtime()
        choreographer.postFrameCallback(callback)
    }

    fun finish() {
        if (!active) return
        active = false
        choreographer.removeFrameCallback(callback)
        DiagnosticRecorder.voiceCenterFrames(frames, slowIntervals, longIntervals)
    }
}
