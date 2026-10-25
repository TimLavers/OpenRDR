package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.main.Api
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

class WebSocketRecoveryTest {
    @Test
    fun `a closed connection is reopened and delivers later frames`() = runBlocking<Unit> {
        // Given
        val server = startServerAndSendFrames(RULE_SESSION_COMPLETED)
        val api = Api(webSocketPort = server.port)
        val deliveredTwice = CompletableDeferred<Unit>()
        var completions = 0
        val connections = mutableListOf<Boolean>()
        val session = launch {
            api.startWebSocketSession({}, {
                if (++completions == 2) deliveredTwice.complete(Unit)
            }, connected = { connections.add(it) })
        }
        try {
            // When
            withTimeout(5_000) { deliveredTwice.await() }

            // Then
            completions shouldBe 2
            connections shouldBe listOf(false, true, false, true)
        } finally {
            session.cancelAndJoin()
            api.client.close()
            server.server.stop(0, 1_000)
        }
        // Then cancellation closes the active socket and stops reconnecting
        connections shouldBe listOf(false, true, false, true, false)
    }
}
