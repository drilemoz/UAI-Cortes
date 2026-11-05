package br.com.uaicortes

import java.security.MessageDigest

internal enum class GeminiTaskKind { LEGENDAS, DESCRICAO }
internal enum class GeminiTaskStage { IN_FLIGHT, REVIEW, READY }

internal data class GeminiTaskCheckpoint(
    val fingerprint: String,
    val model: String,
    val stage: GeminiTaskStage,
    val timestamp: Long,
    val payload: String? = null
)

internal object GeminiTaskRecoveryPolicy {
    const val MAX_BYTES = 1_048_576
    const val MAX_CUES = 1200
    const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    private val sha = Regex("[0-9a-f]{64}")

    fun digest(input: String): String = MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun valid(snapshot: GeminiTaskCheckpoint): Boolean = sha.matches(snapshot.fingerprint) &&
        snapshot.model.length in 1..100 && snapshot.timestamp > 0 &&
        (snapshot.payload?.toByteArray(Charsets.UTF_8)?.size ?: 0) <= MAX_BYTES &&
        (snapshot.stage == GeminiTaskStage.READY) == (snapshot.payload != null)

    fun recover(snapshot: GeminiTaskCheckpoint?, fingerprint: String, now: Long): GeminiTaskCheckpoint? {
        if (snapshot == null || !valid(snapshot) || snapshot.fingerprint != fingerprint ||
            now < snapshot.timestamp || now - snapshot.timestamp > RETENTION_MS) return null
        // Reabrir nunca significa que a chamada anterior falhou ou que está liberada para repetição.
        return if (snapshot.stage == GeminiTaskStage.IN_FLIGHT)
            snapshot.copy(stage = GeminiTaskStage.REVIEW) else snapshot
    }
}

