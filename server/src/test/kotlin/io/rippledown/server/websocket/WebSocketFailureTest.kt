package io.rippledown.server.websocket

import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.ktor.websocket.*
import io.mockk.*
import io.rippledown.model.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketFailureTest {
    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun `a failed connection is retired without disrupting healthy windows`(throws: Boolean, sameUser: Boolean) =
        runTest {
            // Given: a connected client has stopped draining its outgoing socket.
            val manager = WebSocketManager(scope = backgroundScope, sendTimeoutMillis = 100)
            val stalled = mockk<WebSocketSession>()
            val healthy = mockk<WebSocketSession>()
            val received = mutableListOf<Pair<String, Long>>()
            val departures = mutableListOf<UserId>()
            val healthyUser = UserId("healthy")
            val failedUser = if (sameUser) healthyUser else UserId("stalled")
            manager.connectionsChanged = { departures.add(it) }
            every { stalled.incoming } returns Channel<Frame>()
            every { healthy.incoming } returns Channel<Frame>()
            coEvery { stalled.flush() } just Runs
            coEvery { healthy.flush() } just Runs
            coEvery { stalled.send(any<Frame>()) } coAnswers {
                if (firstArg<Frame>() is Frame.Text) {
                    if (throws) throw IllegalStateException("Socket failed")
                    awaitCancellation()
                }
            }
            coEvery { healthy.send(any<Frame>()) } coAnswers {
                val frame = firstArg<Frame>()
                if (frame is Frame.Text) received.add(frame.readText() to testScheduler.currentTime)
            }
            val failedLifetime = backgroundScope.launch { manager.setSession(failedUser, stalled) }
            val healthyLifetime = backgroundScope.launch { manager.setSession(healthyUser, healthy) }
            runCurrent()
            departures.clear()

            // When: several presence/change broadcasts precede a private completion message.
            repeat(3) { manager.broadcast("presence-or-change-$it") }
            manager.sendToUser(healthyUser, "rule-completed")
            runCurrent()
            advanceTimeBy(301)
            runCurrent()

            // Then: the failed connection should cost at most one timeout, not one per queued broadcast.
            received.last().first shouldBe "rule-completed"
            received.last().second shouldBeLessThanOrEqual 100L
            manager.connectedUsers() shouldBe setOf(healthyUser)
            failedLifetime.isCancelled shouldBe true
            healthyLifetime.isActive shouldBe true
            departures shouldBe listOf(failedUser)
        }
}
