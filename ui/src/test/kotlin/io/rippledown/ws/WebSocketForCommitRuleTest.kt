package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.main.Api
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketForCommitRuleTest {
    @Test
    fun `should receive rule session completed message from test websocket using CIO client`() = runBlocking {
        // Given
        val server = startServerAndSendRulesSessionCompleted()
        val api = Api()
        val receivedSignal = CompletableDeferred<Boolean>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = { receivedSignal.complete(true) },
            )
        }

        // Then
        val result = withTimeout(5000) {
            receivedSignal.await()
        }

        result shouldBe true

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        server.stop(1000, 1000)
    }


}