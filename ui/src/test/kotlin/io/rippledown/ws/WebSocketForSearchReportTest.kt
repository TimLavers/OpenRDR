package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.constants.chat.SEARCH_REPORT_PREFIX
import io.rippledown.main.Api
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketForSearchReportTest {
    @Test
    fun `should receive a pushed search report with its prefix removed`() = runBlocking {
        // Given a server that pushes the report of a completed search
        val report = "Search complete. 2 cases found. Lists searched: Processed. Results in: highTSH."
        val serverInfo = startServerAndSendFrames(SEARCH_REPORT_PREFIX + report)
        val api = Api(webSocketPort = serverInfo.port)
        val receivedSignal = CompletableDeferred<String>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {},
                searchReportReceived = { receivedSignal.complete(it) }
            )
        }

        // Then the report arrives with the prefix stripped
        withTimeout(5000) {
            receivedSignal.await()
        } shouldBe report

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        serverInfo.server.stop(1000, 1000)
    }
}
