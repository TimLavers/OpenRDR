package io.rippledown.kb.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.emptyKbGreeting
import io.rippledown.constants.chat.noKbGreeting
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.sample.SampleKB
import io.rippledown.server.KBEndpoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChatCoordinatorTest {
    private lateinit var factory: ChatManagerFactory
    private lateinit var kbService: KnowledgeBaseService
    private lateinit var chatManager: ChatManager
    private lateinit var coordinator: ChatCoordinator
    private val contextChanges = mutableListOf<Pair<UserId, ChatContext>>()
    private val thyroids = KBInfo("thyroids_1", "Thyroids")
    private val glucose = KBInfo("glucose_1", "Glucose")
    private val alice = UserId("alice")
    private val bob = UserId("bob")

    @BeforeTest
    fun setup() {
        factory = mockk()
        kbService = mockk()
        chatManager = mockk()
        coordinator = ChatCoordinator(factory, { kbService }) { userId, context -> contextChanges += userId to context }
    }

    private fun endpointFor(kbInfo: KBInfo): KBEndpoint {
        val endpoint = mockk<KBEndpoint>()
        every { endpoint.kbInfo() } returns kbInfo
        return endpoint
    }

    @Test
    fun `the initial context is no knowledge base`() {
        // When / Then
        coordinator.contextFor(alice) shouldBe ChatContext.NoKnowledgeBase
        coordinator.openEndpointFor(alice) shouldBe null
    }

    @Test
    fun `starting with no knowledge base gives the fixed greeting naming the available knowledge bases`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns listOf(glucose, thyroids)
        every { kbService.demonstrations() } returns emptyList()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
        val greeting = noKbGreeting(listOf("Glucose", "Thyroids"))
        coEvery { chatManager.startConversation(null, greeting) } returns ChatResponse(greeting)

        // When
        val response = coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)

        // Then
        response shouldBe ChatResponse(greeting)
        coordinator.contextFor(alice) shouldBe ChatContext.NoKnowledgeBase
    }

    @Test
    fun `starting with no knowledge base and demonstrations greets with demonstration names`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.demonstrations() } returns SampleKB.demonstrations()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
        val demoTitles = SampleKB.demonstrations().map { it.title() }
        val greeting = noKbGreeting(emptyList(), demoTitles)
        coEvery { chatManager.startConversation(null, greeting) } returns ChatResponse(greeting)

        // When
        val response = coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)

        // Then
        response shouldBe ChatResponse(greeting)
        greeting shouldContain "demonstration knowledge base"
        greeting shouldContain "Zoo Animals"
    }

    @Test
    fun `starting with an empty knowledge base gives the fixed greeting naming it`() = runTest {
        // Given
        val endpoint = endpointFor(thyroids)
        val context = ChatContext.KnowledgeBaseOnly(endpoint)
        every { factory.create(alice, context) } returns chatManager
        val greeting = emptyKbGreeting("Thyroids")
        coEvery { chatManager.startConversation(null, greeting) } returns ChatResponse(greeting)

        // When
        val response = coordinator.startConversation(alice, context)

        // Then
        response shouldBe ChatResponse(greeting)
        coordinator.contextFor(alice) shouldBe context
        coordinator.openEndpointFor(alice) shouldBe endpoint
    }

    @Test
    fun `starting with a case lets the model open the conversation`() = runTest {
        // Given
        val endpoint = mockk<KBEndpoint>()
        val viewableCase = mockk<ViewableCase>()
        val context = ChatContext.CaseInKnowledgeBase(endpoint, viewableCase)
        every { factory.create(alice, context) } returns chatManager
        coEvery { chatManager.startConversation(viewableCase, null) } returns ChatResponse("Shall I add a comment?")

        // When
        val response = coordinator.startConversation(alice, context)

        // Then
        response shouldBe ChatResponse("Shall I add a comment?")
        coordinator.contextFor(alice) shouldBe context
    }

    @Test
    fun `a user message goes to the current chat manager`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.demonstrations() } returns emptyList()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
        coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)
        coEvery { chatManager.response("List the knowledge bases") } returns ChatResponse("A, B")

        // When
        val response = coordinator.responseToUserMessage(alice, "List the knowledge bases")

        // Then
        response shouldBe ChatResponse("A, B")
        coVerify(exactly = 1) { chatManager.response("List the knowledge bases") }
    }

    @Test
    fun `a user message before any conversation is answered without a model`() = runTest {
        // When
        val response = coordinator.responseToUserMessage(alice, "Hello")

        // Then
        response shouldBe ChatResponse(ChatCoordinator.NO_CONVERSATION_MESSAGE)
    }

    @Test
    fun `a user message arriving while a conversation is starting waits for the start to finish`() = runTest {
        // Given
        val startFinished = CompletableDeferred<ChatResponse>()
        val order = mutableListOf<String>()
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.demonstrations() } returns emptyList()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
        coEvery { chatManager.startConversation(null, any()) } coAnswers {
            order += "start"
            startFinished.await()
        }
        coEvery { chatManager.response("Hi") } coAnswers {
            order += "message"
            ChatResponse("Hello")
        }

        // When
        val starting = async { coordinator.startConversation(alice, ChatContext.NoKnowledgeBase) }
        val replying = async { coordinator.responseToUserMessage(alice, "Hi") }
        testScheduler.runCurrent()
        order shouldBe listOf("start")
        startFinished.complete(ChatResponse(""))

        // Then
        starting.await() shouldBe ChatResponse("")
        replying.await() shouldBe ChatResponse("Hello")
        order shouldBe listOf("start", "message")
    }

    @Test
    fun `closing the knowledge base resets the context and drops the chat manager`() = runTest {
        // Given
        val context = ChatContext.KnowledgeBaseOnly(endpointFor(thyroids))
        every { factory.create(alice, context) } returns chatManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
        coordinator.startConversation(alice, context)

        // When
        coordinator.knowledgeBaseClosed(alice)
        val response = coordinator.responseToUserMessage(alice, "Add a comment")

        // Then
        coordinator.contextFor(alice) shouldBe ChatContext.NoKnowledgeBase
        response shouldBe ChatResponse(ChatCoordinator.NO_CONVERSATION_MESSAGE)
        coVerify(exactly = 0) { chatManager.response(any()) }
    }

    @Test
    fun `closing the knowledge base for one user leaves another user on the same KB untouched`() = runTest {
        // Given
        val context = ChatContext.KnowledgeBaseOnly(endpointFor(thyroids))
        val bobsManager = mockk<ChatManager>()
        every { factory.create(alice, context) } returns chatManager
        every { factory.create(bob, context) } returns bobsManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
        coEvery { bobsManager.startConversation(null, any()) } returns ChatResponse("")
        coEvery { bobsManager.response("Hi") } returns ChatResponse("Hello Bob")
        coordinator.startConversation(alice, context)
        coordinator.startConversation(bob, context)

        // When
        coordinator.knowledgeBaseClosed(alice)

        // Then
        coordinator.contextFor(alice) shouldBe ChatContext.NoKnowledgeBase
        coordinator.contextFor(bob) shouldBe context
        coordinator.responseToUserMessage(bob, "Hi") shouldBe ChatResponse("Hello Bob")
    }

    @Test
    fun `closing for a user who never started a conversation is a no-op`() {
        // When
        coordinator.knowledgeBaseClosed(alice)

        // Then
        coordinator.contextFor(alice) shouldBe ChatContext.NoKnowledgeBase
    }

    @Test
    fun `deleting a knowledge base resets exactly the conversations on it and reports them`() = runTest {
        // Given
        val onThyroids = ChatContext.KnowledgeBaseOnly(endpointFor(thyroids))
        val onGlucose = ChatContext.KnowledgeBaseOnly(endpointFor(glucose))
        val carol = UserId("carol")
        val managers = listOf(alice, bob, carol).associateWith { mockk<ChatManager>() }
        every { factory.create(alice, onThyroids) } returns managers.getValue(alice)
        every { factory.create(bob, onGlucose) } returns managers.getValue(bob)
        every { factory.create(carol, onThyroids) } returns managers.getValue(carol)
        managers.values.forEach { coEvery { it.startConversation(null, any()) } returns ChatResponse("") }
        coEvery { managers.getValue(bob).response("Hi") } returns ChatResponse("Hello Bob")
        coordinator.startConversation(alice, onThyroids)
        coordinator.startConversation(bob, onGlucose)
        coordinator.startConversation(carol, onThyroids)
        coordinator.usersOn(thyroids.id) shouldBe setOf(alice, carol)

        // When
        val affected = coordinator.knowledgeBaseDeleted(thyroids.id)

        // Then
        affected shouldBe setOf(alice, carol)
        coordinator.usersOn(thyroids.id) shouldBe emptySet()
        coordinator.contextFor(alice) shouldBe ChatContext.NoKnowledgeBase
        coordinator.contextFor(carol) shouldBe ChatContext.NoKnowledgeBase
        coordinator.contextFor(bob) shouldBe onGlucose
        coordinator.responseToUserMessage(alice, "Hi") shouldBe ChatResponse(ChatCoordinator.NO_CONVERSATION_MESSAGE)
        coordinator.responseToUserMessage(bob, "Hi") shouldBe ChatResponse("Hello Bob")
    }

    @Test
    fun `deleting a knowledge base nobody is on affects no one`() {
        // When / Then
        coordinator.knowledgeBaseDeleted(thyroids.id) shouldBe emptySet()
    }

    @Test
    fun `usersOn excludes users with no knowledge base and users on a case in another KB`() = runTest {
        // Given
        val onThyroidsCase = ChatContext.CaseInKnowledgeBase(endpointFor(thyroids), mockk<ViewableCase>())
        val onGlucose = ChatContext.KnowledgeBaseOnly(endpointFor(glucose))
        val managers = listOf(alice, bob).associateWith { mockk<ChatManager>() }
        every { factory.create(alice, onThyroidsCase) } returns managers.getValue(alice)
        every { factory.create(bob, onGlucose) } returns managers.getValue(bob)
        coEvery { managers.getValue(alice).startConversation(any(), any()) } returns ChatResponse("")
        coEvery { managers.getValue(bob).startConversation(null, any()) } returns ChatResponse("")
        coordinator.startConversation(alice, onThyroidsCase)
        coordinator.startConversation(bob, onGlucose)

        // When / Then
        coordinator.usersOn(thyroids.id) shouldBe setOf(alice)
        coordinator.usersOn(glucose.id) shouldBe setOf(bob)
        coordinator.usersOn("lipids_1") shouldBe emptySet()
    }

    @Test
    fun `each user gets responses from their own chat manager`() = runTest {
        // Given
        val bobsManager = mockk<ChatManager>()
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.demonstrations() } returns emptyList()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
        every { factory.create(bob, ChatContext.NoKnowledgeBase) } returns bobsManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
        coEvery { bobsManager.startConversation(null, any()) } returns ChatResponse("")
        coEvery { chatManager.response("Hi") } returns ChatResponse("Hello Alice")
        coEvery { bobsManager.response("Hi") } returns ChatResponse("Hello Bob")
        coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)
        coordinator.startConversation(bob, ChatContext.NoKnowledgeBase)

        // When / Then
        coordinator.responseToUserMessage(alice, "Hi") shouldBe ChatResponse("Hello Alice")
        coordinator.responseToUserMessage(bob, "Hi") shouldBe ChatResponse("Hello Bob")
        coVerify(exactly = 1) { chatManager.response("Hi") }
        coVerify(exactly = 1) { bobsManager.response("Hi") }
    }

    @Test
    fun `a message from a user with no conversation is answered without a model even when others have one`() =
        runTest {
            // Given
            every { kbService.knowledgeBases() } returns emptyList()
            every { kbService.demonstrations() } returns emptyList()
            every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
            coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
            coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)

            // When
            val response = coordinator.responseToUserMessage(bob, "Hello")

            // Then
            response shouldBe ChatResponse(ChatCoordinator.NO_CONVERSATION_MESSAGE)
            coVerify(exactly = 0) { chatManager.response(any()) }
        }

    @Test
    fun `one user's turn in progress does not block another user's turn`() = runTest {
        // Given
        val aliceFinished = CompletableDeferred<ChatResponse>()
        val bobsManager = mockk<ChatManager>()
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.demonstrations() } returns emptyList()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
        every { factory.create(bob, ChatContext.NoKnowledgeBase) } returns bobsManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
        coEvery { bobsManager.startConversation(null, any()) } returns ChatResponse("")
        coEvery { chatManager.response("Slow") } coAnswers { aliceFinished.await() }
        coEvery { bobsManager.response("Quick") } returns ChatResponse("Done")
        coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)
        coordinator.startConversation(bob, ChatContext.NoKnowledgeBase)

        // When
        val alicesTurn = async { coordinator.responseToUserMessage(alice, "Slow") }
        val bobsTurn = async { coordinator.responseToUserMessage(bob, "Quick") }
        testScheduler.runCurrent()

        // Then
        bobsTurn.isCompleted shouldBe true
        bobsTurn.await() shouldBe ChatResponse("Done")
        alicesTurn.isCompleted shouldBe false
        aliceFinished.complete(ChatResponse("Finally"))
        alicesTurn.await() shouldBe ChatResponse("Finally")
    }

    @Test
    fun `starting a conversation reports the new context for the user's windows to follow`() = runTest {
        // Given
        val context = ChatContext.KnowledgeBaseOnly(endpointFor(thyroids))
        every { factory.create(alice, context) } returns chatManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")

        // When
        coordinator.startConversation(alice, context)

        // Then
        contextChanges shouldBe listOf(alice to context)
    }

    @Test
    fun `a start that fails reports no context change`() = runTest {
        // Given
        val context = ChatContext.KnowledgeBaseOnly(endpointFor(thyroids))
        every { factory.create(alice, context) } throws IllegalStateException("no model")

        // When
        shouldThrow<IllegalStateException> { coordinator.startConversation(alice, context) }

        // Then
        contextChanges shouldBe emptyList()
    }

    @Test
    fun `a user has a conversation once one has been started, whatever its context`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.demonstrations() } returns emptyList()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returns chatManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
        coordinator.hasConversation(alice) shouldBe false

        // When
        coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)

        // Then
        coordinator.hasConversation(alice) shouldBe true
        coordinator.hasConversation(bob) shouldBe false
    }

    @Test
    fun `each start replaces the chat manager`() = runTest {
        // Given
        val first = mockk<ChatManager>()
        val second = mockk<ChatManager>()
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.demonstrations() } returns emptyList()
        every { factory.create(alice, ChatContext.NoKnowledgeBase) } returnsMany listOf(first, second)
        coEvery { first.startConversation(null, any()) } returns ChatResponse("")
        coEvery { second.startConversation(null, any()) } returns ChatResponse("")
        coEvery { second.response("Hi") } returns ChatResponse("From the second")

        // When
        coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)
        coordinator.startConversation(alice, ChatContext.NoKnowledgeBase)
        val response = coordinator.responseToUserMessage(alice, "Hi")

        // Then
        response shouldBe ChatResponse("From the second")
        coVerify(exactly = 0) { first.response(any()) }
    }
}
