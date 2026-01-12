package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketForKbInfoTest {
    @Test
    fun `should receive kb info from test websocket using CIO client`() = runBlocking {
        // Given
        val expectedKbInfo = KBInfo("id123", "Glucose")
        val server = startServerAndSendKbInfo(expectedKbInfo)
        val api = Api()
        val receivedSignal = CompletableDeferred<KBInfo>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {}
            ) { kbInfo -> receivedSignal.complete(kbInfo) }
        }

        // Then
        val result = withTimeout(5000) {
            receivedSignal.await()
        }

        result shouldBe expectedKbInfo

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        server.stop(1000, 1000)
    }
}