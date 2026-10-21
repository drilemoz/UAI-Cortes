package br.com.uaicortes

import android.content.Context
import android.os.Build
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Diagnóstico local opt-in. Nunca grava conteúdo de campos, mídia, URI, ID nem nome de projeto. */
object DiagnosticRecorder {
    private const val PREFS = "local_diagnostic_settings"
    private const val ENABLED = "enabled"
    private const val MAX_BYTES = 512 * 1024
    private const val KEEP_BYTES = 256 * 1024
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "uai-diagnostic-io").apply { isDaemon = true }
    }
    private val lock = Any()
    private val active = AtomicBoolean(false)
    @Volatile private var app: Context? = null
    private var installedHandler = false

    fun initialize(context: Context) {
        app = context.applicationContext
        active.set(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false))
        if (!installedHandler) {
            synchronized(lock) {
                if (!installedHandler) {
                    val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
                    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                        if (active.get()) {
                            // Crash: escreve sincronamente antes de entregar ao handler do Android.
                            runCatching { write("CRASH ${formatThrowable(error)}") }
                        }
                        if (oldHandler != null) oldHandler.uncaughtException(thread, error)
                        else android.os.Process.killProcess(android.os.Process.myPid())
                    }
                    installedHandler = true
                }
            }
        }
        log("SESSION", "app_start sdk=${Build.VERSION.SDK_INT}")
    }

    fun isEnabled(): Boolean = active.get()
    fun setEnabled(value: Boolean) {
        val context = app ?: return
        if (value) {
            active.set(true)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, true).apply()
            log("STATE", "diagnostics_enabled")
        } else {
            log("STATE", "diagnostics_disabled")
            flush()
            active.set(false)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, false).apply()
        }
    }

    fun screen(route: String) = log("SCREEN", DiagnosticPrivacy.safeIdentifier(route))
    fun action(route: String, action: String) = log("ACTION",
        "screen=${DiagnosticPrivacy.safeIdentifier(route)} action=${DiagnosticPrivacy.safeIdentifier(action)}")
    fun tap(route: String, x: Float, y: Float, width: Int, height: Int) = log("TAP",
        "screen=${DiagnosticPrivacy.safeIdentifier(route)} ${DiagnosticPrivacy.touchGrid(x,y,width,height)}")
    fun failure(stage: String, raw: String) = log("ERROR",
        "stage=${DiagnosticPrivacy.safeIdentifier(stage)} ${DiagnosticPrivacy.safeFailure(raw)}")
    fun exception(stage: String, error: Throwable) = log("ERROR",
        "stage=${DiagnosticPrivacy.safeIdentifier(stage)} ${formatThrowable(error)}")

    private fun formatThrowable(error: Throwable): String {
        val types = generateSequence(error) { it.cause }.take(4).map {
            DiagnosticPrivacy.safeIdentifier(it.javaClass.name)
        }.joinToString("->")
        val classification = generateSequence(error) { it.cause }.take(4)
            .map { DiagnosticPrivacy.safeFailure(it.message.orEmpty()) }
            .firstOrNull { !it.endsWith("UNCLASSIFIED") } ?: "NO_MEDIA3_CODE reason=UNCLASSIFIED"
        val trace = error.stackTrace.take(10).joinToString(";") {
            "${DiagnosticPrivacy.safeIdentifier(it.className)}.${DiagnosticPrivacy.safeIdentifier(it.methodName)}:${it.lineNumber}"
        }
        return "types=$types $classification frames=$trace"
    }

    private fun log(kind: String, details: String) {
        if (!active.get()) return
        val line = "${Instant.now()} $kind $details"
        executor.execute { runCatching { write(line) } }
    }

    private fun file(context: Context): File = File(context.filesDir, "uai-diagnostic.txt")

    @Throws(IOException::class)
    private fun write(line: String) = synchronized(lock) {
        val context = app ?: return@synchronized
        val output = file(context)
        output.appendText(line + "\n", Charsets.UTF_8)
        if (output.length() > MAX_BYTES) {
            // Retém linhas completas recentes, com escrita atômica simples via rename.
            val bytes = output.readBytes()
            val start = (bytes.size - KEEP_BYTES).coerceAtLeast(0)
            var from = start
            while (from < bytes.size && bytes[from] != '\n'.code.toByte()) from++
            from = (from + 1).coerceAtMost(bytes.size)
            val rotated = File(output.parentFile, "uai-diagnostic.tmp")
            rotated.writeBytes(bytes.copyOfRange(from, bytes.size))
            if (!rotated.renameTo(output)) {
                output.writeBytes(bytes.copyOfRange(from, bytes.size))
                rotated.delete()
            }
        }
    }

    private fun flush() {
        runCatching { executor.submit {}.get(3, TimeUnit.SECONDS) }
    }

    /** Leitura explícita, para visualizar/exportar apenas quando usuário solicitar. */
    fun report(): String {
        flush()
        val context = app ?: return "Diagnóstico indisponível"
        val body = synchronized(lock) { file(context).takeIf { it.exists() }?.readText(Charsets.UTF_8).orEmpty() }
        return buildString {
            appendLine("Uai Cortes 0.1.41 | diagnóstico local")
            appendLine("Android SDK: ${Build.VERSION.SDK_INT}; aparelho: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Dados: eventos, telas e toques aproximados, sem texto digitado ou caminhos de mídia")
            appendLine("Ativado: ${active.get()}")
            appendLine("---")
            append(body.ifBlank { "Nenhum evento registrado.\n" })
        }
    }

    fun clear() {
        flush()
        synchronized(lock) { app?.let { file(it).delete() } }
        log("STATE", "log_cleared")
    }
}
