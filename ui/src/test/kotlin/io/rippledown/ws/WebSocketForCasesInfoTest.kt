package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.main.Api
import io.rippledown.model.CaseId
import io.rippledown.model.CaseType
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.*
import org.junit.Test

class WebSocketForCasesInfoTest {
    @Test
    fun `should receive cases info from test websocket using CIO client`() = runBlocking {
        // Given
        val expectedCasesInfo = CasesInfo(
            caseIds = listOf(CaseId(id = 1, name = "Case1"), CaseId(id = 2, name = "Case2")),
            kbName = "TestKB"
        )
        val serverInfo = startServerAndSendCasesInfo(expectedCasesInfo)
        val api = Api(webSocketPort = serverInfo.port)
        val receivedSignal = CompletableDeferred<CasesInfo>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {},
                updateCasesInfo = { casesInfo ->
                    receivedSignal.complete(casesInfo)
                }
            )
        }

        // Then
        val result = withTimeout(5000) {
            receivedSignal.await()
        }

        result shouldBe expectedCasesInfo

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        serverInfo.server.stop(1000, 1000)
    }

    @Test
    fun `should receive cases info with cornerstone cases from test websocket`() = runBlocking {
        // Given
        val expectedCasesInfo = CasesInfo(
            caseIds = listOf(CaseId(id = 1, name = "Processed1")),
            cornerstoneCaseIds = listOf(CaseId(id = 2, name = "CS1", type = CaseType.Cornerstone)),
            kbName = "TestKB"
        )
        val serverInfo = startServerAndSendCasesInfo(expectedCasesInfo)
        val api = Api(webSocketPort = serverInfo.port)
        val receivedSignal = CompletableDeferred<CasesInfo>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {},
                updateCasesInfo = { casesInfo ->
                    receivedSignal.complete(casesInfo)
                }
            )
        }

        // Then
        val result = withTimeout(5000) {
            receivedSignal.await()
        }

        result shouldBe expectedCasesInfo
        result.caseIds.size shouldBe 1
        result.cornerstoneCaseIds.size shouldBe 1

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        serverInfo.server.stop(1000, 1000)
    }

    @Test
    fun `should receive empty cases info from test websocket`() = runBlocking {
        // Given
        val expectedCasesInfo = CasesInfo()
        val serverInfo = startServerAndSendCasesInfo(expectedCasesInfo)
        val api = Api(webSocketPort = serverInfo.port)
        val receivedSignal = CompletableDeferred<CasesInfo>()

        // When
        val clientJob = launch {
            api.startWebSocketSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {},
                updateCasesInfo = { casesInfo ->
                    receivedSignal.complete(casesInfo)
                }
            )
        }

        // Then
        val result = withTimeout(5000) {
            receivedSignal.await()
        }

        result shouldBe expectedCasesInfo
        result.count shouldBe 0

        // CLEANUP
        clientJob.cancelAndJoin()
        api.client.close()
        serverInfo.server.stop(1000, 1000)
    }
}
