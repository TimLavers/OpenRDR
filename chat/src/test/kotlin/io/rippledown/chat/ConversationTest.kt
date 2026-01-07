package io.rippledown.chat

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.Content
import dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart
import dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.constants.chat.REASON
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ConversationTest {
    private lateinit var reasonTransformer: ReasonTransformer

    @BeforeEach
    fun setUp() {
        reasonTransformer = mockk(relaxed = true)
        coEvery { reasonTransformer.transform(any<String>()) } returns ReasonTransformation(
            isTransformed = true,
            message = "Transformed successfully"
        )
    }

    @Test
    fun `starting a conversation should delegate to the chat service`() =
        runTest {
            // Given
            val expectedResponse = mockk<GenerateContentResponse>(relaxed = true)
            coEvery { expectedResponse.text } returns "Hello, how can I assist you today?"
            val mockChatService = MockChatService(listOf(expectedResponse))
            val conversation = Conversation(mockChatService, reasonTransformer)

            // When
            val response = conversation.startConversation()

            // Then
            response shouldBe expectedResponse.text
        }

    @Test
    fun `requesting a response should delegate to the chat service`() =
        runTest {
            // Given
            val response1 = mockk<GenerateContentResponse>(relaxed = true)
            val response2 = mockk<GenerateContentResponse>(relaxed = true)
            coEvery { response1.text } returns "Hello, how can I assist you today?"
            coEvery { response2.text } returns "Hello again, how can I help you further?"
            val mockChatService = MockChatService(listOf(response1, response2))
            val conversation = Conversation(mockChatService, reasonTransformer)
            val responseForStartConversation = conversation.startConversation() // Initialize the chat session
            responseForStartConversation shouldBe response1.text

            // When
            val response = conversation.response("Hello, bot!")

            // Then
            response shouldBe response2.text
        }

    @Test
    fun `should handle a function call in a response`() =
        runTest {
            // Given
            val response1 = mockk<GenerateContentResponse>(relaxed = true)
            val response2 = mockk<GenerateContentResponse>(relaxed = true)
            val response3 = mockk<GenerateContentResponse>(relaxed = true)
            coEvery { response1.text } returns "Hello, how can I assist you today?"
            coEvery { response2.text } returns "Your expression was valid"
            coEvery { response2.functionCalls } returns listOf(
                FunctionCallPart(
                    name = "isExpressionValid",
                    args = mapOf("expression" to "x > 0")
                )
            )
            coEvery { response3.text } returns "A beautiful condition was added to the case."
            val mockChatService = MockChatService(listOf(response1, response2, response3))
            val conversation = Conversation(mockChatService, reasonTransformer)
            val responseForStartConversation = conversation.startConversation() // Initialize the chat session
            responseForStartConversation shouldBe response1.text

            // When
            val response = conversation.response("Add the condition 'x > 0'.")

            // Then
            response shouldBe response3.text
        }

    @Test
    fun `should call the expression evaluator if there is a function call in the response`() {
        val userExpression = "x is greater than 0"
        runTest {
            // Given
            val response1 = mockk<GenerateContentResponse>(relaxed = true)
            val response2 = mockk<GenerateContentResponse>(relaxed = true)
            val response3 = mockk<GenerateContentResponse>(relaxed = true)
            coEvery { response1.text } returns "Hello, how can I assist you today?"
            coEvery { response2.text } returns "Your expression was valid"
            coEvery { response2.functionCalls } returns listOf(
                FunctionCallPart(
                    name = TRANSFORM_REASON,
                    args = mapOf(REASON to userExpression)
                )
            )
            coEvery { response3.text } returns "A beautiful condition was added to the case."
            val mockChatService = MockChatService(listOf(response1, response2, response3))
            val conversation = Conversation(mockChatService, reasonTransformer)
            val responseForStartConversation = conversation.startConversation() // Initialize the chat session
            responseForStartConversation shouldBe response1.text

            // When
            conversation.response("Add the condition '$userExpression'.")

            // Then
            coVerify { reasonTransformer.transform(userExpression) }
        }
    }
}

private class MockChatService(private val responses: List<GenerateContentResponse>) : ChatService {
    private var index = 0
    val chat = mockk<Chat>().apply {
        coEvery { sendMessage(any<String>()) } coAnswers {
            responses[index++]
        }
        coEvery { sendMessage(any<Content>()) } coAnswers {
            responses[index++]
        }
    }

    override fun startChat(history: List<Content>) = chat
}