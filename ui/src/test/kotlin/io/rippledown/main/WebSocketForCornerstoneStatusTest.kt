package io.rippledown.main

import io.kotest.matchers.shouldBe
import io.rippledown.mocks.startServer
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketForCornerstoneStatusTest {
    @Test
    fun `test websocket with real CIO client`() = runBlocking {
        // Given
        val expectedStatus = CornerstoneStatus(
            cornerstoneToReview = createViewableCase("Test Case", 1),
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )
        val server = startServer(expectedStatus)
        val api = Api()
        val receivedSignal = CompletableDeferred<CornerstoneStatus>()

        // When
        val clientJob = launch {
            api.startWebSocketSession { cornerstoneStatus ->
                receivedSignal.complete(cornerstoneStatus)
            }
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