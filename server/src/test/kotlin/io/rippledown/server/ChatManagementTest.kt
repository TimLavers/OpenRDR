package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.rippledown.constants.api.SEND_USER_MESSAGE
import io.rippledown.constants.server.CASE_ID
import io.rippledown.constants.server.KB_ID
import kotlin.test.Test

class ChatManagementTest : OpenRDRServerTestBase() {

    @Test
    fun `should delegate generating a bot response to the server application`() = testApplication {
        //Given
        setup()
        val caseId = 42L
        val userMessage = "The report should include a surfing comment"
        val botResponse = "Shall I add a surfing comment to the report?"
        coEvery { kbEndpoint.botResponseToUserMessage(userMessage, caseId) } returns botResponse

        //When
        val result = httpClient.post(SEND_USER_MESSAGE) {
            parameter(KB_ID, kbId)
            parameter(CASE_ID, caseId)
            setBody(userMessage)
        }

        //Then
        coVerify { kbEndpoint.botResponseToUserMessage(userMessage, caseId) }
        result.status shouldBe HttpStatusCode.OK
        result.body<String>() shouldBe botResponse
    }
}