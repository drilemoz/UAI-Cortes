package br.com.uaicortes

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class MediaTaskCoordinatorTest {
    @Test fun cancellationStopsLateMutationAndDoesNotShowAnError() = runBlocking {
        val waiting = CompletableDeferred<Unit>()
        val values = mutableListOf<MediaTaskKind?>()
        var applied = 0
        var failures = 0
        val worker = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val coordinator = MediaTaskCoordinator(worker, { values += it }, { failures++ })
        val origin = MediaTaskOrigin("p1", "media", "append")
        assertTrue(coordinator.start(MediaTaskKind.VIDEO, origin) {
            waiting.await()
            applied++
        })
        assertEquals(MediaTaskKind.VIDEO, coordinator.active)
        assertFalse(coordinator.start(MediaTaskKind.AUDIO, origin) { applied++ })
        assertTrue(coordinator.cancel())
        waiting.complete(Unit)
        yield()
        assertEquals(0, applied)
        assertEquals(0, failures)
        assertEquals(listOf(MediaTaskKind.VIDEO, null), values)
        assertNull(coordinator.active)
        worker.cancel()
    }

    @Test fun destinationChangeCancelsAndAllowsAnotherImport() = runBlocking {
        val waiting = CompletableDeferred<Unit>()
        val worker = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val coordinator = MediaTaskCoordinator(worker, {}, { throw it })
        val origin = MediaTaskOrigin("p1", "media", "new")
        assertTrue(coordinator.start(MediaTaskKind.VIDEO, origin) { waiting.await() })
        assertFalse(coordinator.cancelIfDestinationChanged("p1", "media", "new"))
        assertTrue(coordinator.cancelIfDestinationChanged("p1", "home", "new"))
        assertTrue(coordinator.start(MediaTaskKind.VIDEO, origin) {})
        yield()
        assertNull(coordinator.active)
        worker.cancel()
    }

    @Test fun realFailureIsReportedOnceAndReleasesTheGate() = runBlocking {
        val worker = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val errors = mutableListOf<String?>()
        val coordinator = MediaTaskCoordinator(worker, {}, { errors += it.message })
        assertTrue(coordinator.start(MediaTaskKind.AUDIO, MediaTaskOrigin("p", "editor", "add")) {
            error("metadata failure")
        })
        yield()
        assertEquals(listOf("metadata failure"), errors)
        assertNull(coordinator.active)
        worker.cancel()
    }
}
