package io.rippledown.chat

import com.google.common.collect.ImmutableList
import com.google.genai.Chat
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentResponse
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.Conversation.Companion.MAX_EMPTY_RESPONSE_RETRIES
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import java.util.*
import kotlin.test.Test

class HandleResponseTest {
    private lateinit var functionCallHandlers: Map<String, FunctionCallHandler>
    private val handledFunctions = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        handledFunctions.clear()
        functionCallHandlers = mapOf(
            TRANSFORM_REASON to object : FunctionCallHandler {
                override suspend fun handle(args: Map<String, Any?>): String {
                    val reason = args[REASON_PARAMETER]?.toString() ?: ""
                    handledFunctions.add(reason)
                    return "Transformed: $reason"
                }
            },
            "getSuggestedConditions" to object : FunctionCallHandler {
                override suspend fun handle(args: Map<String, Any?>): String {
                    handledFunctions.add("getSuggestedConditions")
                    return "1. condition A\n2. condition B"
                }
            }
        )
    }

    private fun mockResponse(
        text: String? = null,
        functionCalls: List<FunctionCall>? = null
    ): GenerateContentResponse {
        val response = mockk<GenerateContentResponse>()
        every { response.text() } returns text
        every { response.functionCalls() } returns functionCalls?.let { ImmutableList.copyOf(it) }
        every { response.candidates() } returns null
        every { response.usageMetadata() } returns null
        return response
    }

    private fun functionCall(name: String, args: Map<String, Any> = emptyMap()): FunctionCall {
        val fc = mockk<FunctionCall>()
        every { fc.name() } returns Optional.of(name)
        every { fc.args() } returns Optional.of(args)
        return fc
    }

    private fun createConversation(vararg subsequentResponses: GenerateContentResponse): Conversation {
        val initResponse = mockResponse(text = "Hello")
        val allResponses = listOf(initResponse) + subsequentResponses.toList()
        val chatService = QueuedChatService(allResponses)
        val conversation = Conversation(chatService, functionCallHandlers)
        kotlinx.coroutines.runBlocking { conversation.startConversation() }
        return conversation
    }

    // --- Text response tests ---

    @Test
    fun `should return text when response contains text`() = runTest {
        // Given
        val conversation = createConversation()
        val response = mockResponse(text = "Here are some suggestions.")

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Here are some suggestions."
    }

    @Test
    fun `should return empty string when response text is empty`() = runTest {
        // Given
        val conversation = createConversation()
        val response = mockResponse(text = "")

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe ""
    }

    @Test
    fun `should strip enclosing json markers from text`() = runTest {
        // Given
        val conversation = createConversation()
        val response = mockResponse(text = "```json\n{\"action\": \"UserAction\"}\n```")

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "{\"action\": \"UserAction\"}"
    }

    // --- Function call tests ---

    @Test
    fun `should execute function call and return final text`() = runTest {
        // Given
        val finalResponse = mockResponse(text = "Condition added successfully.")
        val conversation = createConversation(finalResponse)
        val response = mockResponse(
            functionCalls = listOf(
                functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to "x is high"))
            )
        )

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Condition added successfully."
        handledFunctions shouldBe listOf("x is high")
    }

    @Test
    fun `should execute multiple function calls in a single response`() = runTest {
        // Given
        val finalResponse = mockResponse(text = "Both conditions added.")
        val conversation = createConversation(finalResponse)
        val response = mockResponse(
            functionCalls = listOf(
                functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to "x is high")),
                functionCall("getSuggestedConditions")
            )
        )

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Both conditions added."
        handledFunctions shouldBe listOf("x is high", "getSuggestedConditions")
    }

    @Test
    fun `should handle multiple rounds of function calls`() = runTest {
        // Given
        val secondRoundResponse = mockResponse(
            functionCalls = listOf(
                functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to "y is low"))
            )
        )
        val finalResponse = mockResponse(text = "All done.")
        val conversation = createConversation(secondRoundResponse, finalResponse)
        val response = mockResponse(
            functionCalls = listOf(
                functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to "x is high"))
            )
        )

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "All done."
        handledFunctions shouldBe listOf("x is high", "y is low")
    }

    @Test
    fun `should return unknown function message for unregistered function`() = runTest {
        // Given
        val finalResponse = mockResponse(text = "Continuing.")
        val conversation = createConversation(finalResponse)
        val unknownFc = mockk<FunctionCall>()
        every { unknownFc.name() } returns Optional.of("nonExistentFunction")
        every { unknownFc.args() } returns Optional.of(emptyMap())
        val response = mockResponse(functionCalls = listOf(unknownFc))

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Continuing."
    }

    // --- Empty response and retry tests ---

    @Test
    fun `should retry and succeed when first response is empty but retry has text`() = runTest {
        // Given
        val retryResponse = mockResponse(text = "Here is the response after retry.")
        val conversation = createConversation(retryResponse)
        val emptyResponse = mockResponse()

        // When
        val result = conversation.handleResponse(response = emptyResponse)

        // Then
        result shouldBe "Here is the response after retry."
    }

    @Test
    fun `should retry and succeed when retry response contains function calls`() = runTest {
        // Given
        val functionCallResponse = mockResponse(
            functionCalls = listOf(functionCall("getSuggestedConditions"))
        )
        val finalResponse = mockResponse(text = "Suggestions presented.")
        val conversation = createConversation(functionCallResponse, finalResponse)
        val emptyResponse = mockResponse()

        // When
        val result = conversation.handleResponse(response = emptyResponse)

        // Then
        result shouldBe "Suggestions presented."
        handledFunctions shouldBe listOf("getSuggestedConditions")
    }

    @Test
    fun `should exhaust retries and return fallback when all retries return empty`() = runTest {
        // Given
        val emptyRetryResponses = (1..MAX_EMPTY_RESPONSE_RETRIES).map { mockResponse() }
        val conversation = createConversation(*emptyRetryResponses.toTypedArray())
        val emptyResponse = mockResponse()

        // When
        val result = conversation.handleResponse(response = emptyResponse)

        // Then
        result shouldBe "No function call or text response"
    }

    @Test
    fun `should succeed on last retry attempt`() = runTest {
        // Given
        val emptyRetries = (1 until MAX_EMPTY_RESPONSE_RETRIES).map { mockResponse() }
        val successResponse = mockResponse(text = "Finally got a response.")
        val conversation = createConversation(*(emptyRetries + successResponse).toTypedArray())
        val emptyResponse = mockResponse()

        // When
        val result = conversation.handleResponse(response = emptyResponse)

        // Then
        result shouldBe "Finally got a response."
    }

    @Test
    fun `should respect emptyResponseRetries parameter and not retry when already at max`() = runTest {
        // Given
        val conversation = createConversation()
        val emptyResponse = mockResponse()

        // When
        val result = conversation.handleResponse(
            response = emptyResponse,
            emptyResponseRetries = MAX_EMPTY_RESPONSE_RETRIES
        )

        // Then
        result shouldBe "No function call or text response"
    }

    // --- Function calls followed by empty response ---

    @Test
    fun `should retry when function calls resolve to empty response`() = runTest {
        // Given
        val emptyAfterFunctionCall = mockResponse()
        val retryResponse = mockResponse(text = "Recovered after function call.")
        val conversation = createConversation(emptyAfterFunctionCall, retryResponse)
        val response = mockResponse(
            functionCalls = listOf(
                functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to "x is high"))
            )
        )

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Recovered after function call."
        handledFunctions shouldBe listOf("x is high")
    }

    // --- Null function calls list ---

    @Test
    fun `should treat null function calls list as no function calls`() = runTest {
        // Given
        val conversation = createConversation()
        val response = mockResponse(text = "Just text, no functions.")

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Just text, no functions."
    }

    @Test
    fun `should treat empty function calls list as no function calls`() = runTest {
        // Given
        val conversation = createConversation()
        val response = mockk<GenerateContentResponse>()
        every { response.text() } returns "Text with empty function list."
        every { response.functionCalls() } returns ImmutableList.of()

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Text with empty function list."
    }

    // --- Response with both text and function calls ---

    @Test
    fun `should process function calls before returning text`() = runTest {
        // Given
        val afterFunctionCallResponse = mockResponse(text = "Function processed, here is the result.")
        val conversation = createConversation(afterFunctionCallResponse)
        val response = mockResponse(
            text = "This text should be ignored because there are function calls.",
            functionCalls = listOf(functionCall("getSuggestedConditions"))
        )

        // When
        val result = conversation.handleResponse(response)

        // Then
        result shouldBe "Function processed, here is the result."
        handledFunctions shouldBe listOf("getSuggestedConditions")
    }
}

private class QueuedChatService(private val responses: List<GenerateContentResponse>) : ChatService {
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
