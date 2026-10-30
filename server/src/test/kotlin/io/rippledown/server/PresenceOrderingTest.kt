package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.PRESENCE_PREFIX
import io.rippledown.fromJsonString
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.model.KBInfo
import io.rippledown.model.PresenceInfo
import io.rippledown.model.UserId
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class PresenceOrderingTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `a delayed presence calculation cannot overwrite newer membership`(joining: Boolean) {
        // Given: a presence calculation waits for the real KB lock.
        val alice = UserId("Alice")
        val bob = UserId("Bob")
        val connected = AtomicReference(if (joining) setOf(alice) else setOf(alice, bob))
        val sessionLookedUp = CountDownLatch(1)
        val messages = Collections.synchronizedList(mutableListOf<String>())
        val sockets = mockk<WebSocketManager>()
        val open = mockk<OpenKnowledgeBases>()
        every { open.usersOn("clinic") } returns setOf(alice, bob)
        every { sockets.connectedUsers() } answers {
            connected.get()
        }
        every { sockets.broadcast(any()) } answers { messages.add(firstArg()) }
        val session = KBSession(KB(InMemoryKB(KBInfo("clinic", "Clinic"))))
        val presence = Presence(open, sockets) { sessionLookedUp.countDown(); session }
        lateinit var olderPush: Thread

        // When: Bob's connection changes and the lock holder publishes the newer snapshot first.
        session.locked {
            olderPush = thread(name = "older-presence") { presence.push("clinic") }
            check(sessionLookedUp.await(5, TimeUnit.SECONDS))
            connected.set(if (joining) setOf(alice, bob) else setOf(alice))
            presence.push("clinic")
        }
        olderPush.join(5_000)

        // Then: the final broadcast must still describe the current presence.
        olderPush.isAlive shouldBe false
        messages.size shouldBe 2
        messages.last().removePrefix(PRESENCE_PREFIX).fromJsonString<PresenceInfo>()
            .users.map { it.userId } shouldBe if (joining) listOf("Alice", "Bob") else listOf("Alice")
    }
}
