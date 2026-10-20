package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.CaseTestUtils
import io.rippledown.kb.chat.ChatContext
import io.rippledown.kb.chat.ChatManager
import io.rippledown.kb.chat.ChatManagerFactory
import io.rippledown.model.UserId
import io.rippledown.model.chat.ChatResponse
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ServerApplicationResetTest {
    private val alice = UserId("alice")
    private val bob = UserId("bob")
    private val persistence = InMemoryPersistenceProvider()
    private val sockets = mockk<WebSocketManager>()
    private val gate = TestRequestGate(drainTimeoutMillis = 100)

    @AfterEach
    fun cleanup() = unmockkConstructor(ChatManagerFactory::class)

    private fun application(): ServerApplication {
        mockkConstructor(ChatManagerFactory::class)
        val manager = mockk<ChatManager>()
        every { anyConstructed<ChatManagerFactory>().create(any(), any()) } returns manager
        coEvery { manager.startConversation(any(), any()) } returns ChatResponse("hello")
        return ServerApplication(persistence, sockets, gate)
    }

    @Test
    fun `reset removes persisted KBs endpoints and conversations including no-KB conversations`() = runTest {
        // Given
        every { sockets.connectedUsers() } returns emptySet()
        val app = application()
        val first = app.createKB("First", false)
        app.createKB("Second", false)
        app.chatCoordinator.startConversation(alice, ChatContext.KnowledgeBaseOnly(app.kbFor(first)))
        app.chatCoordinator.startConversation(bob, ChatContext.NoKnowledgeBase)

        // When
        app.reset()
        app.reset()

        // Then
        app.kbList() shouldBe emptyList()
        persistence.idStore().data() shouldBe emptyMap()
        shouldThrow<IllegalArgumentException> { app.kbFor(first) }
        app.chatCoordinator.hasConversation(alice) shouldBe false
        app.chatCoordinator.hasConversation(bob) shouldBe false
        app.createKB("First", false).name shouldBe "First"
    }

    @Test
    fun `a connected user rejects reset before any KB or conversation is removed`() = runTest {
        // Given
        every { sockets.connectedUsers() } returns setOf(alice)
        val app = application()
        val kb = app.createKB("First", false)
        app.chatCoordinator.startConversation(alice, ChatContext.KnowledgeBaseOnly(app.kbFor(kb)))
        val ruleManager = app.kbFor(kb).session.ruleSessionManagerFor(alice)
        ruleManager.startRuleSessionToAddComment(CaseTestUtils.createCase("Case1"), "Go")

        // When
        shouldThrow<ResetRejectedException> { app.reset() }

        // Then
        app.kbList() shouldBe listOf(kb)
        persistence.idStore().data().keys shouldBe setOf(kb.id)
        app.kbFor(kb).kbInfo() shouldBe kb
        app.chatCoordinator.hasConversation(alice) shouldBe true
        ruleManager.isRuleSessionActive() shouldBe true
        gate.enterRequest() shouldBe false
    }

    @Test
    fun `reset cancels every users active rule session before deleting their KB`() = runTest {
        // Given
        every { sockets.connectedUsers() } returns emptySet()
        val app = application()
        val kb = app.createKB("First", false)
        val session = app.kbFor(kb).session
        val managers = listOf(alice, bob).map { user ->
            session.ruleSessionManagerFor(user).apply {
                startRuleSessionToAddComment(CaseTestUtils.createCase("Case1"), "Go")
            }
        }
        session.usersEditing() shouldBe setOf(alice, bob)

        // When
        app.reset()

        // Then
        managers.forEach { it.isRuleSessionActive() shouldBe false }
        app.kbList() shouldBe emptyList()
        persistence.idStore().data() shouldBe emptyMap()
    }
}
