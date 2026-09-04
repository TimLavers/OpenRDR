package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.constants.api.START_CONVERSATION
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.kb.chat.ChatContext
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import kotlin.test.Test

class ChatManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `starting a conversation with a KB and a case starts it in the case context`() = testApplication {
        //Given
        setupServer()
        val caseId = 42L
        val viewableCase = mockk<ViewableCase>()
        every { kbEndpoint.viewableCase(caseId) } returns viewableCase
        val response = ChatResponse("Shall I add a surfing comment to the report?")
        val context = slot<ChatContext>()
        coEvery { chatCoordinator.startConversation(capture(context)) } returns response

        //When
        val result = httpClient.post(START_CONVERSATION) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, caseId)
        }

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
        context.captured shouldBe ChatContext.CaseInKnowledgeBase(kbEndpoint, viewableCase)
    }

    @Test
    fun `starting a conversation with a KB but no case starts it in the KB-only context`() = testApplication {
        //Given
        setupServer()
        val response = ChatResponse("The knowledge base has no cases.")
        val context = slot<ChatContext>()
        coEvery { chatCoordinator.startConversation(capture(context)) } returns response

        //When
        val result = httpClient.post(START_CONVERSATION) {
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
        coEvery { chatCoordinator.startConversation(capture(context)) } returns response

        //When
        val result = httpClient.post(START_CONVERSATION)

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
        context.captured shouldBe ChatContext.NoKnowledgeBase
    }

    @Test
    fun `a user message goes to the coordinator, whatever ids accompany it`() = testApplication {
        //Given
        setupServer()
        val userMessage = "The report should include a surfing comment"
        val response = ChatResponse("Shall I add a surfing comment to the report?")
        coEvery { chatCoordinator.responseToUserMessage(userMessage) } returns response

        //When
        val result = httpClient.post(SEND_USER_MESSAGE) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, 42L)
            setBody(userMessage)
        }

        //Then
        coVerify { chatCoordinator.responseToUserMessage(userMessage) }
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
    }

    @Test
    fun `a user message with no ids goes to the coordinator`() = testApplication {
        //Given
        setupServer()
        val userMessage = "List the knowledge bases"
        val response = ChatResponse("Glucose\nThyroids")
        coEvery { chatCoordinator.responseToUserMessage(userMessage) } returns response

        //When
        val result = httpClient.post(SEND_USER_MESSAGE) {
            setBody(userMessage)
        }

        //Then
        result.status shouldBe HttpStatusCode.OK
        result.body<ChatResponse>() shouldBe response
    }
}