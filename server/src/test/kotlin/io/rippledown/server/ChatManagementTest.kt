package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import io.rippledown.constants.api.CHAT_CONTEXT
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.constants.api.START_CONVERSATION
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.kb.chat.ChatContext
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.UserId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatContextInfo
import io.rippledown.model.chat.ChatResponse
import kotlin.test.Test

class ChatManagementTest : OpenRDRServerTestBase() {
    private val carol = UserId("carol")

    @Test
    fun `starting a conversation with a KB and a case starts it in the case context`() = testApplication {
        //Given
        setupServer()
        val caseId = 42L
        val viewableCase = mockk<ViewableCase>()
        every { kbEndpoint.viewableCase(caseId) } returns viewableCase
        val response = ChatResponse("Shall I add a surfing comment to the report?")
        val context = slot<ChatContext>()
        coEvery { chatCoordinator.startConversation(UserId("alice"), capture(context)) } returns response

        //When
        val result = clientFor(UserId("alice")).post(START_CONVERSATION) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, caseId)
        }

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
        context.captured shouldBe ChatContext.CaseInKnowledgeBase(kbEndpoint, viewableCase)
    }

    @Test
    fun `a request without the identity header is refused`() = testApplication {
        //Given
        setupServer()
        val anonymousClient = createClient { }

        //When
        val result = anonymousClient.post(START_CONVERSATION)

        //Then
        result.status shouldBe HttpStatusCode.InternalServerError
        coVerify(exactly = 0) { chatCoordinator.startConversation(any(), any()) }
    }

    @Test
    fun `starting a conversation with a KB but no case starts it in the KB-only context`() = testApplication {
        //Given
        setupServer()
        val response = ChatResponse("The knowledge base has no cases.")
        val context = slot<ChatContext>()
        coEvery { chatCoordinator.startConversation(carol, capture(context)) } returns response

        //When
        val result = clientFor(carol).post(START_CONVERSATION) {
            parameter(KB_ID, kbId)
        }

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
        context.captured shouldBe ChatContext.KnowledgeBaseOnly(kbEndpoint)
    }

    @Test
    fun `starting a conversation with no ids starts it in the no-KB context`() = testApplication {
        //Given
        setupServer()
        val response = ChatResponse("No knowledge base is open.")
        val context = slot<ChatContext>()
        coEvery { chatCoordinator.startConversation(carol, capture(context)) } returns response

        //When
        val result = clientFor(carol).post(START_CONVERSATION)

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
        context.captured shouldBe ChatContext.NoKnowledgeBase
    }

    @Test
    fun `a user with no conversation has no context to report`() = testApplication {
        //Given
        setupServer()
        every { chatCoordinator.hasConversation(carol) } returns false

        //When
        val result = clientFor(carol).get(CHAT_CONTEXT)

        //Then
        result.status shouldBe HttpStatusCode.NoContent
    }

    @Test
    fun `the context of a user's conversation is reported with its KB and case`() = testApplication {
        //Given
        setupServer()
        val kbInfo = KBInfo(kbId, kbName)
        every { kbEndpoint.kbInfo() } returns kbInfo
        val viewableCase = mockk<ViewableCase>()
        val case = mockk<RDRCase>()
        every { viewableCase.case } returns case
        every { case.id } returns 42L
        every { chatCoordinator.hasConversation(carol) } returns true
        every { chatCoordinator.contextFor(carol) } returns ChatContext.CaseInKnowledgeBase(kbEndpoint, viewableCase)

        //When
        val result = clientFor(carol).get(CHAT_CONTEXT)

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatContextInfo>() shouldBe ChatContextInfo(kbInfo, 42L)
    }

    @Test
    fun `a conversation with no knowledge base is reported as an empty context`() = testApplication {
        //Given
        setupServer()
        every { chatCoordinator.hasConversation(carol) } returns true
        every { chatCoordinator.contextFor(carol) } returns ChatContext.NoKnowledgeBase

        //When
        val result = clientFor(carol).get(CHAT_CONTEXT)

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatContextInfo>() shouldBe ChatContextInfo()
    }

    @Test
    fun `a user message goes to the coordinator, whatever ids accompany it`() = testApplication {
        //Given
        setupServer()
        val userMessage = "The report should include a surfing comment"
        val response = ChatResponse("Shall I add a surfing comment to the report?")
        coEvery { chatCoordinator.responseToUserMessage(UserId("bob"), userMessage) } returns response

        //When
        val result = clientFor(UserId("bob")).post(SEND_USER_MESSAGE) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, 42L)
            setBody(userMessage)
        }

        //Then
        coVerify { chatCoordinator.responseToUserMessage(UserId("bob"), userMessage) }
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
    }

    @Test
    fun `a user message with no ids goes to the coordinator`() = testApplication {
        //Given
        setupServer()
        val userMessage = "List the knowledge bases"
        val response = ChatResponse("Glucose\nThyroids")
        coEvery { chatCoordinator.responseToUserMessage(carol, userMessage) } returns response

        //When
        val result = clientFor(carol).post(SEND_USER_MESSAGE) {
            setBody(userMessage)
        }

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
    }
}