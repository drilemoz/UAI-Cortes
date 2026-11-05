package br.com.uaicortes

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Ritmo para lotes INTERATIVOS. Só aplica limites manuais do projeto/modelo atual;
 * não consulta cotas oficiais, não roda em background e não faz chamadas de rede.
 * O uso fora deste aparelho pode reduzir a capacidade real.
 */
internal class GeminiLocalPacing(context: Context) {
    private val keys = GeminiCredentialStore(context)
    private val ledger = GeminiUsageLedger(context)
    private val quotas = GeminiQuotaStore(context)

    suspend fun beforeRequest(model: String, onWait: suspend (Long) -> Unit) {
        val project = keys.profiles().firstOrNull { it.id == keys.activeId() }
            ?.projectId.orEmpty()
        if (project.isBlank() || model.isBlank()) return
        val limits = quotas.get(project, model)
        if (!limits.configured()) return
        // Uma execução foreground pode aguardar a janela por minuto; nunca esperar até o dia seguinte.
        while (true) {
            coroutineContext.ensureActive()
            val events = ledger.events()
            val forecast = GeminiQuotaPolicy.compute(events, project, model, "narracao", limits)
            if (limits.rpd != null && forecast.remainingToday == 0) {
                error("O limite diário configurado para este projeto/modelo foi alcançado neste aparelho. " +
                    "Revise o AI Studio e retome o lote quando houver capacidade.")
            }
            val waitMs = GeminiQuotaPolicy.rpmWaitMs(events, project, model, limits.rpm) ?: break
            onWait(waitMs)
            delay(waitMs)
        }
    }
}
