package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.constants.chat.PRESENCE_PREFIX
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.main.Api
import io.rippledown.model.PresenceInfo
import io.rippledown.model.PresentUser
import io.rippledown.toJsonString
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test

class WebSocketForPresenceTest {
    @Test
    fun `presence frames reach the callback and malformed presence does not stop subsequent frames`() =
        runBlocking<Unit> {
            // Given
            val present = PresenceInfo("clinic", listOf(PresentUser("Alice", true), PresentUser("Bob", false)))
            val empty = PresenceInfo("clinic", emptyList())
            val server = startServerAndSendFrames(
                PRESENCE_PREFIX + present.toJsonString(),
                PRESENCE_PREFIX + "not json",
                PRESENCE_PREFIX + empty.toJsonString(),
                RULE_SESSION_COMPLETED
            )
            val api = Api(webSocketPort = server.port)
            val received = mutableListOf<PresenceInfo>()
            val completed = CompletableDeferred<Unit>()
            var completions = 0
            var otherCallbacks = 0
            try {
                // When
                val session = launch {
                    api.startWebSocketSession(
                        updateCornerstoneStatus = { ++otherCallbacks },
                        ruleSessionCompleted = {
                            ++completions
                            completed.complete(Unit)
                        },
                        updateCasesInfo = { ++otherCallbacks },
                        kbInfoUpdated = { ++otherCallbacks },
                        kbClosed = { ++otherCallbacks },
                        chatContextChanged = { ++otherCallbacks },
                        presenceChanged = { received.add(it) }
                    )
                }

                withTimeout(5_000) { completed.await() }
                session.cancelAndJoin()

                // Then
                received shouldBe listOf(present, empty)
                completions shouldBe 1
                otherCallbacks shouldBe 0
            } finally {
                api.client.close()
                server.server.stop(0, 1_000)
            }
        }
}
