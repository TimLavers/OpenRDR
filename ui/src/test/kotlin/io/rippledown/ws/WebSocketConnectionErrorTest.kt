package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.websocket.*
import io.rippledown.main.WebSocketApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketConnectionErrorTest {
    @Test
    fun `should handle WebSocket connection error`() = runTest {
        // Given
        val mockEngine = MockEngine.Companion { _ ->
            throw Exception("Connection failed")
        }
        val client = createMockHttpClient(mockEngine)
        val webSocketManager = WebSocketApi(client)
        var errorOccurred = false

        // When
        try {
            webSocketManager.startSession(
                updateCornerstoneStatus = {},
                ruleSessionCompleted = {}
            )
        } catch (_: Exception) {
            errorOccurred = true
        }

        // Then
        errorOccurred shouldBe true
    }

    private fun createMockHttpClient(engine: MockEngine) = HttpClient(engine) {
        install(WebSockets)
    }
}