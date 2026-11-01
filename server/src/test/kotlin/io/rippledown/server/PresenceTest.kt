package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.chat.PRESENCE_PREFIX
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.model.*
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.toJsonString
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class PresenceTest {
    private val alice = UserId("alice")
    private val bob = UserId("bob")
    private val carol = UserId("carol")
    private val thyroids = "thyroids_1"
    private val glucose = "glucose_1"
    private lateinit var openKnowledgeBases: OpenKnowledgeBases
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var thyroidsSession: KBSession
    private lateinit var presence: Presence

    @BeforeTest
    fun setup() {
        openKnowledgeBases = mockk()
        webSocketManager = mockk()
        every { webSocketManager.broadcast(any()) } returns Unit
        thyroidsSession = KBSession(KB(InMemoryKB(KBInfo(thyroids, "Thyroids"))))
        presence = Presence(openKnowledgeBases, webSocketManager) { if (it == thyroids) thyroidsSession else null }
    }

    private fun startEditing(userId: UserId) {
        val kb = thyroidsSession.kb
        val builder = RDRCaseBuilder()
        builder.addValue(kb.attributeManager.getOrCreate("TSH"), defaultDate, "1.0")
        thyroidsSession.ruleSessionManagerFor(userId).startRuleSessionToAddComment(builder.build("Case"), "Go.")
    }

    @Test
    fun `those present are the users on the KB who are connected`() {
        // Given
        every { openKnowledgeBases.usersOn(thyroids) } returns setOf(alice, bob, carol)
        every { webSocketManager.connectedUsers() } returns setOf(alice, carol, UserId("dave"))

        // When / Then
        presence.present(thyroids) shouldBe listOf(PresentUser("alice", false), PresentUser("carol", false))
    }

    @Test
    fun `a user on another KB is not present`() {
        // Given
        every { openKnowledgeBases.usersOn(thyroids) } returns setOf(alice)
        every { openKnowledgeBases.usersOn(glucose) } returns setOf(bob)
        every { webSocketManager.connectedUsers() } returns setOf(alice, bob)

        // When / Then
        presence.present(thyroids) shouldBe listOf(PresentUser("alice", false))
        presence.present(glucose) shouldBe listOf(PresentUser("bob", false))
    }

    @Test
    fun `a user with a rule session in progress is editing`() {
        // Given
        every { openKnowledgeBases.usersOn(thyroids) } returns setOf(alice, bob)
        every { webSocketManager.connectedUsers() } returns setOf(alice, bob)
        startEditing(bob)

        // When / Then
        presence.present(thyroids) shouldBe listOf(PresentUser("alice", false), PresentUser("bob", true))
    }

    @Test
    fun `nobody is editing a KB that has no session`() {
        // Given
        every { openKnowledgeBases.usersOn(glucose) } returns setOf(alice)
        every { webSocketManager.connectedUsers() } returns setOf(alice)

        // When / Then
        presence.present(glucose) shouldBe listOf(PresentUser("alice", false))
    }

    @Test
    fun `users are listed in a stable order`() {
        // Given
        every { openKnowledgeBases.usersOn(thyroids) } returns setOf(carol, alice, bob)
        every { webSocketManager.connectedUsers() } returns setOf(bob, carol, alice)

        // When / Then
        presence.present(thyroids).map { it.userId } shouldBe listOf("alice", "bob", "carol")
    }

    @Test
    fun `push broadcasts the presence of the KB`() {
        // Given
        every { openKnowledgeBases.usersOn(thyroids) } returns setOf(alice)
        every { webSocketManager.connectedUsers() } returns setOf(alice)

        // When
        presence.push(thyroids)

        // Then
        val expected = PresenceInfo(thyroids, listOf(PresentUser("alice", false)))
        verify(exactly = 1) { webSocketManager.broadcast(PRESENCE_PREFIX + expected.toJsonString<PresenceInfo>()) }
    }

    @Test
    fun `a transition pushes for the KB left and the KB joined`() {
        // Given
        every { openKnowledgeBases.usersOn(any()) } returns emptySet()
        every { webSocketManager.connectedUsers() } returns emptySet()

        // When
        presence.transition(thyroids, glucose)

        // Then
        verify(exactly = 1) { webSocketManager.broadcast(messageFor(thyroids)) }
        verify(exactly = 1) { webSocketManager.broadcast(messageFor(glucose)) }
    }

    @Test
    fun `a transition within one KB pushes once, and one between nothing and nothing not at all`() {
        // Given
        every { openKnowledgeBases.usersOn(any()) } returns emptySet()
        every { webSocketManager.connectedUsers() } returns emptySet()

        // When
        presence.transition(thyroids, thyroids)
        presence.transition(null, null)

        // Then
        verify(exactly = 1) { webSocketManager.broadcast(any()) }
        verify(exactly = 1) { webSocketManager.broadcast(messageFor(thyroids)) }
    }

    @Test
    fun `a transition from or to nothing pushes for the one KB`() {
        // Given
        every { openKnowledgeBases.usersOn(any()) } returns emptySet()
        every { webSocketManager.connectedUsers() } returns emptySet()

        // When
        presence.transition(null, glucose)
        presence.transition(thyroids, null)

        // Then
        verify(exactly = 1) { webSocketManager.broadcast(messageFor(glucose)) }
        verify(exactly = 1) { webSocketManager.broadcast(messageFor(thyroids)) }
    }

    private fun messageFor(kbId: String) =
        PRESENCE_PREFIX + PresenceInfo(kbId, emptyList()).toJsonString<PresenceInfo>()
}
