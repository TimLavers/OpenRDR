package io.rippledown.server

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertNotNull

class ServerChatServiceTest {

    @Test
    fun  `the service should recognise a request for the list of KBs`() = runTest{
        val chatService = ServerChatServiceFactory().createChatService()
        val chat = chatService.startChat(listOf())
        val response = chat.sendMessage("What KBs are available?")
        extractAction(response.text!!) shouldBe  "ListKnowledgeBases"
    }

    @Test
    fun `chat service should recognise when it receives a message not meant for it`() = runTest {
        val chatService = ServerChatServiceFactory().createChatService()
        val chat = chatService.startChat(listOf())
        val response = chat.sendMessage("Please put glucose above TSH")
        extractAction(response.text!!) shouldBe  "KnowledgeBaseEdit"
    }

    private fun extractAction(response: String): String? {
        val jsonText  = response.substringAfter("json").substringBefore("```").trim()
        val jsonObject = Json.parseToJsonElement(jsonText).jsonObject
        assertNotNull(jsonObject["action"], "Action field not found in JSON")
        return jsonObject["action"]?.jsonPrimitive?.content
    }
}
