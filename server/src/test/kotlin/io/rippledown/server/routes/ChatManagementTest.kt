package io.rippledown.server.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.constants.api.START_CONVERSATION
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.ServerChatResult
import io.rippledown.server.OpenRDRServerTestBase
import kotlin.test.Test

class ChatManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate starting a conversation with model to the server application`() = testApplication {
        TODO()
        //Given
        setupServer()
        val caseId = 42L
        val response = "Shall I add a surfing comment to the report?"
//        coEvery { kbEndpoint.startConversation(caseId) } returns response

        //When
        val result = httpClient.post(START_CONVERSATION) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, caseId)
        }

        //Then
//        coVerify { kbEndpoint.startConversation(caseId) }
        result.status shouldBe HttpStatusCode.Companion.OK
    }

    @Test
    fun `should delegate generating a response from the model to the server application`() = testApplication {
        TODO()
        //Given
        setupServer()
        val userMessage = "This is a request to the server."
        val response = "Shall I add a surfing comment to the report?"
//        coEvery { serverApplication.processUserRequest(userMessage, null) } returns ServerChatResult(response)

        //When
        val result = httpClient.post(SEND_USER_MESSAGE) {
            setBody(userMessage)
        }

        //Then
        coVerify { serverApplication.processUserRequest(userMessage, null) }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.body<ServerChatResult>().userMessage shouldBe response
    }
}