package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.emptyKbGreeting
import io.rippledown.constants.chat.noKbGreeting
import io.rippledown.model.KBInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.server.KBEndpoint
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChatCoordinatorTest {
    private lateinit var factory: ChatManagerFactory
    private lateinit var kbService: KnowledgeBaseService
    private lateinit var chatManager: ChatManager
    private lateinit var coordinator: ChatCoordinator
    private val thyroids = KBInfo("thyroids_1", "Thyroids")
    private val glucose = KBInfo("glucose_1", "Glucose")

    @BeforeTest
    fun setup() {
        factory = mockk()
        kbService = mockk()
        chatManager = mockk()
        coordinator = ChatCoordinator(factory, kbService)
    }

    @Test
    fun `the initial context is no knowledge base`() {
        // When / Then
        coordinator.context() shouldBe ChatContext.NoKnowledgeBase
    }

    @Test
    fun `starting with no knowledge base gives the fixed greeting naming the available knowledge bases`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns listOf(glucose, thyroids)
        every { factory.create(ChatContext.NoKnowledgeBase) } returns chatManager
        val greeting = noKbGreeting(listOf("Glucose", "Thyroids"))
        coEvery { chatManager.startConversation(null, greeting) } returns ChatResponse(greeting)

        // When
        val response = coordinator.startConversation(ChatContext.NoKnowledgeBase)

        // Then
        response shouldBe ChatResponse(greeting)
        coordinator.context() shouldBe ChatContext.NoKnowledgeBase
    }

    @Test
    fun `starting with an empty knowledge base gives the fixed greeting naming it`() = runTest {
        // Given
        val endpoint = mockk<KBEndpoint>()
        every { endpoint.kbInfo() } returns thyroids
        val context = ChatContext.KnowledgeBaseOnly(endpoint)
        every { factory.create(context) } returns chatManager
        val greeting = emptyKbGreeting("Thyroids")
        coEvery { chatManager.startConversation(null, greeting) } returns ChatResponse(greeting)

        // When
        val response = coordinator.startConversation(context)

        // Then
        response shouldBe ChatResponse(greeting)
        coordinator.context() shouldBe context
    }

    @Test
    fun `starting with a case lets the model open the conversation`() = runTest {
        // Given
        val endpoint = mockk<KBEndpoint>()
        val viewableCase = mockk<ViewableCase>()
        val context = ChatContext.CaseInKnowledgeBase(endpoint, viewableCase)
        every { factory.create(context) } returns chatManager
        coEvery { chatManager.startConversation(viewableCase, null) } returns ChatResponse("Shall I add a comment?")

        // When
        val response = coordinator.startConversation(context)

        // Then
        response shouldBe ChatResponse("Shall I add a comment?")
        coordinator.context() shouldBe context
    }

    @Test
    fun `a user message goes to the current chat manager`() = runTest {
        // Given
        every { kbService.knowledgeBases() } returns emptyList()
        every { factory.create(ChatContext.NoKnowledgeBase) } returns chatManager
        coEvery { chatManager.startConversation(null, any()) } returns ChatResponse("")
        coordinator.startConversation(ChatContext.NoKnowledgeBase)
        coEvery { chatManager.response("List the knowledge bases") } returns ChatResponse("A, B")

        // When
        val response = coordinator.responseToUserMessage("List the knowledge bases")

        // Then
        response shouldBe ChatResponse("A, B")
        coVerify(exactly = 1) { chatManager.response("List the knowledge bases") }
    }

    @Test
    fun `a user message before any conversation is answered without a model`() = runTest {
        // When
        val response = coordinator.responseToUserMessage("Hello")

        // Then
        response shouldBe ChatResponse(ChatCoordinator.NO_CONVERSATION_MESSAGE)
    }

    @Test
    fun `each start replaces the chat manager`() = runTest {
        // Given
        val first = mockk<ChatManager>()
        val second = mockk<ChatManager>()
        every { kbService.knowledgeBases() } returns emptyList()
        every { factory.create(ChatContext.NoKnowledgeBase) } returnsMany listOf(first, second)
        coEvery { first.startConversation(null, any()) } returns ChatResponse("")
        coEvery { second.startConversation(null, any()) } returns ChatResponse("")
        coEvery { second.response("Hi") } returns ChatResponse("From the second")

        // When
        coordinator.startConversation(ChatContext.NoKnowledgeBase)
        coordinator.startConversation(ChatContext.NoKnowledgeBase)
        val response = coordinator.responseToUserMessage("Hi")

        // Then
        response shouldBe ChatResponse("From the second")
        coVerify(exactly = 0) { first.response(any()) }
    }
}
