package io.rippledown.ws

import io.kotest.matchers.shouldBe
import io.rippledown.constants.chat.KB_CHANGED_PREFIX
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.main.Api
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.model.change.KbChange
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test

class WebSocketForKbChangeTest {
    @Test
    fun `changes are routed through Api and a malformed frame does not stop later messages`() = runBlocking<Unit> {
        // Given
        val persisted = KbChange("clinic", ChangeRecord(7, "Bob", 1L, ChangeKind.RULES_ADDED, "Added a rule.", setOf(3), setOf(9)))
        val unpersisted = KbChange("other", ChangeRecord(null, "Alice", 2L, ChangeKind.DESCRIPTION_SET, "Set the description."))
        val server = startServerAndSendFrames(
            KB_CHANGED_PREFIX + persisted.toJsonString(),
            KB_CHANGED_PREFIX + "not json",
            KB_CHANGED_PREFIX + unpersisted.toJsonString(),
            RULE_SESSION_COMPLETED
        )
        val api = Api(webSocketPort = server.port)
        val received = mutableListOf<KbChange>()
        var completions = 0
        var otherCallbacks = 0
        try {
            // When
            withTimeout(5_000) {
                api.startWebSocketSession(
                    updateCornerstoneStatus = { ++otherCallbacks },
                    ruleSessionCompleted = { ++completions },
                    updateCasesInfo = { ++otherCallbacks },
                    kbInfoUpdated = { ++otherCallbacks },
                    kbClosed = { ++otherCallbacks },
                    chatContextChanged = { ++otherCallbacks },
                    presenceChanged = { ++otherCallbacks },
                    kbChanged = { received.add(it) }
                )
            }

            // Then
            received shouldBe listOf(persisted, unpersisted)
            completions shouldBe 1
            otherCallbacks shouldBe 0
        } finally {
            api.client.close()
            server.server.stop(0, 1_000)
        }
    }
}
