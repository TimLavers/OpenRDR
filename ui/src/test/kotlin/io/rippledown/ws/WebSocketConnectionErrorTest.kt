package io.rippledown.ws

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.websocket.*
import io.rippledown.main.WebSocketApi
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

class WebSocketConnectionErrorTest {
    @Test
    fun `connection failures back off from one second to thirty and propagate cancellation`() = runBlocking<Unit> {
        // Given
        val client = HttpClient(MockEngine { throw IllegalStateException("Offline") }) { install(WebSockets) }
        val delays = mutableListOf<Long>()
        val connections = mutableListOf<Boolean>()
        val socket = WebSocketApi(client, retryDelay = {
            delays.add(it)
            if (delays.size == 7) throw CancellationException("Stop")
        })
        try {
            // When
            val failure = shouldThrow<CancellationException> {
                socket.startSession({}, {}, connected = { connections.add(it) })
            }

            // Then
            failure.message shouldBe "Stop"
            delays shouldBe listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L)
            connections shouldBe listOf(false)
        } finally {
            client.close()
        }
    }

    @Test
    fun `cancellation while backing off prevents another attempt`() = runBlocking<Unit> {
        // Given
        val client = HttpClient(MockEngine { throw IllegalStateException("Offline") }) { install(WebSockets) }
        val waiting = CompletableDeferred<Unit>()
        var retries = 0
        val socket = WebSocketApi(client, retryDelay = {
            ++retries
            waiting.complete(Unit)
            awaitCancellation()
        })
        val session = launch { socket.startSession({}, {}) }
        try {
            withTimeout(5_000) { waiting.await() }

            // When
            session.cancelAndJoin()

            // Then
            session.isCancelled shouldBe true
            retries shouldBe 1
        } finally {
            session.cancelAndJoin()
            client.close()
        }
    }
}
