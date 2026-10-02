package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.rippledown.constants.server.USER_ID_HEADER
import io.rippledown.main.Api
import io.rippledown.model.UserId
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketHandshakeUserIdTest {
    @Test
    fun `the web-socket handshake carries the user id header`() = runBlocking {
        // Given
        val handshakeHeaders = CompletableDeferred<Headers>()
        val serverInfo = startServerRecordingHandshakeHeaders(handshakeHeaders)
        val api = Api(webSocketPort = serverInfo.port, userId = UserId("alice"))

        // When
        val clientJob = launch {
            api.startWebSocketSession(updateCornerstoneStatus = {}, ruleSessionCompleted = {})
        }
        val headers = withTimeout(5000) { handshakeHeaders.await() }

        // Then
        headers[USER_ID_HEADER] shouldBe "alice"

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        serverInfo.server.stop(1000, 1000)
    }
}
