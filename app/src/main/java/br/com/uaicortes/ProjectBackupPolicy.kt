package br.com.uaicortes

import java.security.MessageDigest

internal object ProjectBackupPolicy {
    val intervals = listOf(15, 30, 60, 120, 300)
    val counts = listOf(3, 5, 10)
    fun interval(value: Int): Int = value.takeIf { it in intervals } ?: 30
    fun count(value: Int): Int = value.takeIf { it in counts } ?: 5
    fun hash(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
