package br.com.uaicortes

import android.content.Context
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.os.Build
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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
    private val lastSaveLogMs = AtomicLong(0L)
    private val lastTapLogMs = AtomicLong(0L)
    private val lastVoicePerfMs = AtomicLong(0L)
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
        if (active.get()) executor.execute { inspectPreviousProcessExit(context.applicationContext) }
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
    fun action(route: String, action: String) {
        // Deslizamentos de PiP/áudio podem salvar dezenas de vezes por segundo.
        // Agregação apenas da telemetria: NÃO interfere no salvamento real do projeto.
        if (action == "project_saved") {
            val now = System.currentTimeMillis()
            val previous = lastSaveLogMs.get()
            if (now - previous < 1500L || !lastSaveLogMs.compareAndSet(previous, now)) return
        }
        log("ACTION", "screen=${DiagnosticPrivacy.safeIdentifier(route)} action=${DiagnosticPrivacy.safeIdentifier(action)}")
    }

    /** Somente códigos/taxonomia controlada; nunca token, URL, prompt ou corpo HTTP. */
    fun api(service: String, operation: String, model: String?, status: Int?,
        elapsedMs: Long?, tokens: Long? = null, outcome: String = "unknown") {
        if (!active.get()) return
        val event = ApiDiagnosticPolicy.sanitize(service, operation, model, status,
            elapsedMs, tokens, outcome)
        log("API", event)
    }
    /** Limita taps repetitivos sem impedir o registro de SCREEN, ACTION e ERROR. */
    fun tap(route: String, x: Float, y: Float, width: Int, height: Int) {
        if (!active.get()) return
        val now = System.currentTimeMillis()
        val previous = lastTapLogMs.get()
        if (now - previous < 250L || !lastTapLogMs.compareAndSet(previous, now)) return
        log("TAP", "screen=${DiagnosticPrivacy.safeIdentifier(route)} " +
            DiagnosticPrivacy.touchGrid(x,y,width,height))
    }

    /** Telemetria de atualização sem URL, nome de APK ou headers de autenticação. */
    fun update(event: String, bytes: Long? = null) {
        val allowed = setOf("started", "pause_requested", "paused", "resumed", "stop_requested",
            "stopped", "resume_full_restart", "interrupted", "verified", "failed")
        val safe = event.takeIf { it in allowed } ?: "unknown"
        val mib = bytes?.takeIf { it >= 0L }?.div(1024L * 1024L)
        log("UPDATE", "state=$safe${mib?.let { " size_mib=$it" }.orEmpty()}")
    }

    /** Métricas agregadas da Central de Vozes; sem consulta, nome, ID ou conteúdo de fala. */
    fun voiceCenterPerformance(stage: String, elapsedMs: Long, count: Int) {
        if (!active.get()) return
        if (stage !in setOf("open", "cache", "filter", "page")) return
        val now = System.currentTimeMillis()
        if (stage == "filter" || stage == "cache") {
            val previous = lastVoicePerfMs.get()
            if (now - previous < 10_000L || !lastVoicePerfMs.compareAndSet(previous, now)) return
        }
        val usedHeapMiB = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
            (1024L * 1024L)).coerceAtLeast(0L)
        log("PERF", "screen=voice_center stage=$stage elapsed_ms=${elapsedMs.coerceIn(0L, 60_000L)} " +
            "count=${count.coerceIn(0, 500)} heap_mib=$usedHeapMiB")
    }

    /** Apenas amostragem curta de intervalos entre callbacks; não equivalem a frames renderizados nem ANR. */
    fun voiceCenterFrames(sampled: Int, slow: Int, long: Int) {
        if (!active.get() || sampled < 2) return
        log("PERF", "screen=voice_center stage=frame_sample sampled=${sampled.coerceIn(0, 90)} " +
            "slow_intervals=${slow.coerceIn(0, 90)} long_intervals=${long.coerceIn(0, 90)}")
    }

    /** Somente quantidade agregada de fontes indisponíveis, sem URI ou títulos. */
    fun mediaAvailability(missing: Int) {
        if (missing > 0) log("MEDIA", "state=inaccessible count=${missing.coerceIn(1, 100)}")
    }

    /** Somente dimensões, qualidade e estágio de processamento, nunca nome do vídeo. */
    fun exportEvent(stage: String, requestedShortSide: Int? = null,
                    progress: Int? = null, elapsedMs: Long? = null) {
        val phase = stage.takeIf { it in setOf("start", "rendering", "saving", "success", "failure", "cancel") }
            ?: "unknown"
        val quality = requestedShortSide?.takeIf { it in listOf(360,480,720,1080) }
        val percent = progress?.coerceIn(0,100)
        val seconds = elapsedMs?.takeIf { it >= 0L }?.div(1000L)
        log("EXPORT", "stage=$phase${quality?.let { " quality=$it" }.orEmpty()}" +
            "${percent?.let { " percent=$it" }.orEmpty()}" +
            "${seconds?.let { " elapsed_s=$it" }.orEmpty()}")
    }

    /** Causa do encerramento ANTERIOR, quando Android 11+ informa. Não examina dump privado. */
    private fun inspectPreviousProcessExit(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !active.get()) return
        runCatching {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return@runCatching
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastReported = prefs.getLong("last_exit_timestamp", 0L)
            val info = manager.getHistoricalProcessExitReasons(null, 0, 5)
                .filter { it.timestamp > lastReported && it.timestamp <= System.currentTimeMillis() }
                .maxByOrNull { it.timestamp } ?: return@runCatching
            val reason = when (info.reason) {
                ApplicationExitInfo.REASON_ANR -> "anr"
                ApplicationExitInfo.REASON_CRASH -> "java_crash"
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "native_crash"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "low_memory"
                ApplicationExitInfo.REASON_SIGNALED -> "signal"
                ApplicationExitInfo.REASON_USER_REQUESTED -> "user_requested"
                ApplicationExitInfo.REASON_EXIT_SELF -> "exit_self"
                else -> "other"
            }
            write("${Instant.now()} PROCESS_EXIT reason=$reason")
            prefs.edit().putLong("last_exit_timestamp", info.timestamp).apply()
        }
    }

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
            val appVersion = runCatching {
                @Suppress("DEPRECATION")
                val installedPackage = context.packageManager.getPackageInfo(context.packageName, 0)
                installedPackage.versionName.orEmpty()
            }.getOrDefault("desconhecida")
            appendLine("Uai Cortes $appVersion | diagnóstico local")
            appendLine("Android SDK: ${Build.VERSION.SDK_INT}; aparelho: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Dados: eventos, erros e códigos de API; sem segredos, prompts ou caminhos de mídia")
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
