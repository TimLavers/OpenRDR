package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.main.Api
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketForCornerstoneStatusTest {
    @Test
    fun `should receive cornerstone status from test websocket using CIO client`() = runBlocking {
        // Given
        val expectedStatus = CornerstoneStatus(
            cornerstoneToReview = createViewableCase("Test Case", 1),
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )
        val server = startServerAndSendCornerstoneStatus(expectedStatus)
        val api = Api()
        val receivedSignal = CompletableDeferred<CornerstoneStatus>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(updateCornerstoneStatus = { cornerstoneStatus ->
                receivedSignal.complete(cornerstoneStatus)
            }, ruleSessionCompleted = {})
        }

        // Then
        val result = withTimeout(5000) {
            receivedSignal.await()
        }

        result shouldBe expectedStatus

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        server.stop(1000, 1000)
    }


}