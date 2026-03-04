package io.rippledown.chat

import com.google.common.collect.ImmutableList
import com.google.genai.Chat
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentResponse
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import java.util.*
import kotlin.test.Test

class ConversationTest {
    private lateinit var reasonTransformer: ReasonTransformer
    private lateinit var functionCallHandlers: Map<String, FunctionCallHandler>

    @BeforeEach
    fun setUp() {
        reasonTransformer = mockk()
        coEvery { reasonTransformer.transform(any<String>()) } returns ReasonTransformation(
            reasonId = 42,
            message = "Transformed successfully"
        )
        functionCallHandlers = mapOf(
            TRANSFORM_REASON to object : FunctionCallHandler {
                override suspend fun handle(args: Map<String, Any?>): String {
                    val reason = args[REASON_PARAMETER]?.toString() ?: ""
                    val transformation = reasonTransformer.transform(reason)
                    return "'$reason' evaluation: ${transformation.reasonId}"
                }
            }
        )
    }

    private fun mockResponse(text: String? = null, functionCalls: List<FunctionCall>? = null): GenerateContentResponse {
        val response = mockk<GenerateContentResponse>()
        every { response.text() } returns text
        every { response.functionCalls() } returns functionCalls?.let { ImmutableList.copyOf(it) }
        return response
    }

    private fun functionCall(name: String, args: Map<String, Any>): FunctionCall {
        val fc = mockk<FunctionCall>()
        every { fc.name() } returns Optional.of(name)
        every { fc.args() } returns Optional.of(args)
        return fc
    }

    @Test
    fun `starting a conversation should delegate to the chat service`() =
        runTest {
            // Given
            val expectedResponse = mockResponse(text = "Hello, how can I assist you today?")
            val mockChatService = MockChatService(listOf(expectedResponse))
            val conversation = Conversation(mockChatService, functionCallHandlers)

            // When
            val response = conversation.startConversation()

            // Then
            response shouldBe "Hello, how can I assist you today?"
        }

    @Test
    fun `requesting a response should delegate to the chat service`() =
        runTest {
            // Given
            val response1 = mockResponse(text = "Hello, how can I assist you today?")
            val response2 = mockResponse(text = "Hello again, how can I help you further?")
            val mockChatService = MockChatService(listOf(response1, response2))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            val responseForStartConversation = conversation.startConversation() // Initialize the chat session
            responseForStartConversation shouldBe "Hello, how can I assist you today?"

            // When
            val response = conversation.response("Hello, bot!")

            // Then
            response shouldBe "Hello again, how can I help you further?"
        }

    @Test
    fun `should handle a function call in a response`() =
        runTest {
            // Given
            val response1 = mockResponse(text = "Hello, how can I assist you today?")
            val response2 = mockResponse(
                text = "Your expression was valid",
                functionCalls = listOf(
                    functionCall("isExpressionValid", mapOf("expression" to "x > 0"))
                )
            )
            val response3 = mockResponse(text = "A beautiful condition was added to the case.")
            val mockChatService = MockChatService(listOf(response1, response2, response3))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            val responseForStartConversation = conversation.startConversation() // Initialize the chat session
            responseForStartConversation shouldBe "Hello, how can I assist you today?"

            // When
            val response = conversation.response("Add the condition 'x > 0'.")

            // Then
            response shouldBe "A beautiful condition was added to the case."
        }

    @Test
    fun `should call the expression evaluator if there is a function call in the response`() {
        val userExpression = "x is greater than 0"
        runTest {
            // Given
            val response1 = mockResponse(text = "Hello, how can I assist you today?")
            val response2 = mockResponse(
                text = "Your expression was valid",
                functionCalls = listOf(
                    functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to userExpression))
                )
            )
            val response3 = mockResponse(text = "A beautiful condition was added to the case.")
            val mockChatService = MockChatService(listOf(response1, response2, response3))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            val responseForStartConversation = conversation.startConversation() // Initialize the chat session
            responseForStartConversation shouldBe "Hello, how can I assist you today?"

            // When
            conversation.response("Add the condition '$userExpression'.")

            // Then
            coVerify { reasonTransformer.transform(userExpression) }
        }
    }

    @Test
    fun `should handle multiple rounds of function calls`() =
        runTest {
            // Given
            val firstExpression = "hite > 1"
            val secondExpression = "height > 1"
            val response1 = mockResponse(text = "Hello, how can I assist you today?")
            val response2 = mockResponse(
                functionCalls = listOf(
                    functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to firstExpression))
                )
            )
            val response3 = mockResponse(
                functionCalls = listOf(
                    functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to secondExpression))
                )
            )
            val response4 = mockResponse(text = "The condition was added.")
            val mockChatService = MockChatService(listOf(response1, response2, response3, response4))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            conversation.startConversation()

            // When
            val response = conversation.response("Add the condition '$firstExpression'.")

            // Then
            response shouldBe "The condition was added."
            coVerify { reasonTransformer.transform(firstExpression) }
            coVerify { reasonTransformer.transform(secondExpression) }
        }
}

private class MockChatService(private val responses: List<GenerateContentResponse>) : ChatService {
    private var index = 0
    val chat = mockk<Chat>().apply {
        every { sendMessage(any<String>()) } answers {
            responses[index++]
        }
        every { sendMessage(any<Content>()) } answers {
            responses[index++]
        }
    }

    override fun startChat() = chat
}