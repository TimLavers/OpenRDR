package io.rippledown.server.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.ServerChatResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test

class KnowledgeBaseEditMessageActionTest : ServerActionTestBase() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }
/*
    @Test
    fun `should pass message to KB chat and return result when kbId is provided`() = runBlocking{
        val instruction = "Do something awesome!"
        val kbId = "Id123"
        val response = "Here's the response"
        every { actionsInterface.passUserMessageToKbChat(instruction, kbId) }.returns(response)
        val jsonObject = JsonObject(
            mapOf(
                "action" to JsonPrimitive("KnowledgeBaseEditMessage"),
                "userMessage" to JsonPrimitive(instruction)
            )
        )
        val received = KnowledgeBaseEditMessageAction(jsonObject).applyAction(actionsInterface, kbId)
        received shouldBe ServerChatResult(response)
        verify { actionsInterface.passUserMessageToKbChat(instruction, kbId) }
    }

    @Test
    fun `should return error message and not call KB chat when kbId is null`() = runBlocking {
        val instruction = "Do something awesome!"
        every { actionsInterface.passUserMessageToKbChat(instruction, any()) }.returns("Whatever")
        val jsonObject = JsonObject(
            mapOf(
                "action" to JsonPrimitive("KnowledgeBaseEditMessage"),
                "userMessage" to JsonPrimitive(instruction)
            )
        )
        val received = KnowledgeBaseEditMessageAction(jsonObject).applyAction(actionsInterface, null)
        received shouldBe ServerChatResult("Instruction not understood.")
        verify(exactly = 0) { actionsInterface.passUserMessageToKbChat(instruction, any()) }
    }
 */
}
