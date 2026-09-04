package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.constants.chat.KB_CLOSED
import io.rippledown.constants.chat.KB_INFO_PREFIX
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import io.rippledown.toJsonString
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketForKbInfoTest {
    @Test
    fun `a KbInfo frame is delivered to kbInfoUpdated`() = runBlocking {
        // Given
        val thyroids = KBInfo("thyroids_1", "Thyroids")
        val serverInfo = startServerAndSendFrames(KB_INFO_PREFIX + thyroids.toJsonString<KBInfo>())
        val api = Api(webSocketPort = serverInfo.port)
        val received = CompletableDeferred<KBInfo>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {},
                kbInfoUpdated = { received.complete(it) }
            )
        }

        // Then
        withTimeout(5000) { received.await() } shouldBe thyroids

        clientJob.cancelAndJoin()
        api.client.close()
        serverInfo.server.stop(1000, 1000)
    }

    @Test
    fun `a KbClosed frame is delivered to kbClosed`() = runBlocking {
        // Given
        val serverInfo = startServerAndSendFrames(KB_CLOSED)
        val api = Api(webSocketPort = serverInfo.port)
        val received = CompletableDeferred<Boolean>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {},
                kbClosed = { received.complete(true) }
            )
        }

        // Then
        withTimeout(5000) { received.await() } shouldBe true

        clientJob.cancelAndJoin()
        api.client.close()
        serverInfo.server.stop(1000, 1000)
    }
}
