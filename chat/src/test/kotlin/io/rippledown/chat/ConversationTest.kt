package io.rippledown.chat

import com.google.common.collect.ImmutableList
import com.google.genai.Chat
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.Conversation.Companion.CONTINUE_NUDGE
import io.rippledown.chat.Conversation.Companion.OPENING_PROMPT
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
        every { response.usageMetadata() } returns Optional.empty()
        return response
    }

    private fun functionCall(name: String, args: Map<String, Any>): FunctionCall {
        val fc = mockk<FunctionCall>()
        every { fc.name() } returns Optional.of(name)
        every { fc.args() } returns Optional.of(args)
        return fc
    }

    private fun malformedFunctionCallResponse(): GenerateContentResponse {
        val failure =
            IllegalArgumentException("The response finished unexpectedly with reason MALFORMED_FUNCTION_CALL.")
        val response = mockk<GenerateContentResponse>()
        every { response.functionCalls() } throws failure
        every { response.text() } throws failure
        every { response.usageMetadata() } returns Optional.empty()
        return response
    }

    @Test
    fun `starting a conversation starts the chat and sends nothing to the model`() =
        runTest {
            // Given
            val mockChatService = MockChatService(listOf(mockResponse(text = "should not be requested")))
            val conversation = Conversation(mockChatService, functionCallHandlers)

            // When
            val response = conversation.startConversation()

            // Then
            response shouldBe ""
            mockChatService.sentMessages shouldBe emptyList()
            mockChatService.sentContents shouldBe emptyList()
            mockChatService.chatStarted shouldBe true
        }

    @Test
    fun `with no opening turn the first message is sent alone`() =
        runTest {
            // Given
            val mockChatService = MockChatService(listOf(mockResponse(text = "Answer")))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            conversation.startConversation()

            // When
            val response = conversation.response("Question")

            // Then
            response shouldBe "Answer"
            mockChatService.sentMessages shouldBe listOf("Question")
            mockChatService.sentContents shouldBe emptyList()
        }

    @Test
    fun `the opening turn goes ahead of the first user message as the model's own turn, and only once`() =
        runTest {
            // Given
            val mockChatService = MockChatService(listOf(mockResponse(text = "First"), mockResponse(text = "Second")))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            conversation.startConversation(openingTurn = "Would you like to add a comment?")

            // When
            val first = conversation.response("Yes")
            val second = conversation.response("Add \"Surf.\"")

            // Then
            first shouldBe "First"
            second shouldBe "Second"
            mockChatService.sentContents.size shouldBe 1
            val contents = mockChatService.sentContents.single()
            contents.map { it.role().get() } shouldBe listOf("user", "model", "user")
            contents.map { it.singleText() } shouldBe listOf(OPENING_PROMPT, "Would you like to add a comment?", "Yes")
            mockChatService.sentMessages shouldBe listOf("Add \"Surf.\"")
        }

    @Test
    fun `the opening turn is still sent if the first send fails before reaching the model`() =
        runTest {
            // Given
            val contents = mutableListOf<List<Content>>()
            val chat = mockk<Chat>()
            every { chat.sendMessage(capture(contents)) } answers {
                if (contents.size == 1) throw NoSuchElementException("No value present")
                mockResponse(text = "Recovered")
            }
            val conversation = Conversation(object : ChatService {
                override fun startChat() = chat
            }, functionCallHandlers)
            conversation.startConversation(openingTurn = "Greeting")

            // When
            val response = conversation.response("Hello")

            // Then
            response shouldBe "Recovered"
            contents.size shouldBe 2
            contents.last().map { it.role().get() } shouldBe listOf("user", "model", "user")
            contents.last().last().singleText() shouldContain CONTINUE_NUDGE
        }

    @Test
    fun `requesting a response should delegate to the chat service`() =
        runTest {
            // Given
            val mockChatService =
                MockChatService(listOf(mockResponse(text = "Hello again, how can I help you further?")))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            conversation.startConversation()

            // When
            val response = conversation.response("Hello, bot!")

            // Then
            response shouldBe "Hello again, how can I help you further?"
        }

    @Test
    fun `should handle a function call in a response`() =
        runTest {
            // Given
            val response2 = mockResponse(
                text = "Your expression was valid",
                functionCalls = listOf(
                    functionCall("isExpressionValid", mapOf("expression" to "x > 0"))
                )
            )
            val response3 = mockResponse(text = "A beautiful condition was added to the case.")
            val mockChatService = MockChatService(listOf(response2, response3))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            conversation.startConversation()

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
            val response2 = mockResponse(
                text = "Your expression was valid",
                functionCalls = listOf(
                    functionCall(TRANSFORM_REASON, mapOf(REASON_PARAMETER to userExpression))
                )
            )
            val response3 = mockResponse(text = "A beautiful condition was added to the case.")
            val mockChatService = MockChatService(listOf(response2, response3))
            val conversation = Conversation(mockChatService, functionCallHandlers)
            conversation.startConversation()

            // When
            conversation.response("Add the condition '$userExpression'.")

            // Then
            coVerify { reasonTransformer.transform(userExpression) }
        }
    }

    @Test
    fun `should recover when the SDK returns a malformed function call by retrying with a nudge`() =
        runTest {
            // Given the first send returns a response whose finish reason causes google-genai's
            // checkFinishReason to throw IllegalArgumentException (MALFORMED_FUNCTION_CALL).
            // The nudged retry then succeeds.
            val recovered = mockResponse(text = "The comment was added.")
            val messages = mutableListOf<String>()
            val chat = mockk<Chat>()
            every { chat.sendMessage(capture(messages)) } answers {
                when (messages.size) {
                    1 -> malformedFunctionCallResponse()
                    else -> recovered
                }
            }
            val conversation = Conversation(object : ChatService {
                override fun startChat() = chat
            }, functionCallHandlers)
            conversation.startConversation()

            // When
            val response = conversation.response("Add the comment \"Comment 1.\"")

            // Then
            response shouldBe "The comment was added."
            messages.last() shouldContain CONTINUE_NUDGE
        }

    @Test
    fun `should recover when the SDK throws on an empty candidate by retrying with a nudge`() =
        runTest {
            // Given the first send throws the google-genai SDK's NoSuchElementException
            // (a candidate with no content), and the nudged retry succeeds.
            val recovered = mockResponse(text = "The comment was replaced.")
            val messages = mutableListOf<String>()
            val chat = mockk<Chat>()
            every { chat.sendMessage(capture(messages)) } answers {
                when (messages.size) {
                    1 -> throw NoSuchElementException("No value present") // empty candidate content
                    else -> recovered // nudged retry
                }
            }
            val conversation = Conversation(object : ChatService {
                override fun startChat() = chat
            }, functionCallHandlers)
            conversation.startConversation()

            // When
            val response = conversation.response("Replace the comment by \"Comment 2.\"")

            // Then
            response shouldBe "The comment was replaced."
            messages.last() shouldContain CONTINUE_NUDGE
        }

    @Test
    fun `should handle multiple rounds of function calls`() =
        runTest {
            // Given
            val firstExpression = "hite > 1"
            val secondExpression = "height > 1"
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
            val mockChatService = MockChatService(listOf(response2, response3, response4))
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

private fun Content.singleText() = parts().get().single().text().get()

private class MockChatService(private val responses: List<GenerateContentResponse>) : ChatService {
    private var index = 0
    val sentMessages = mutableListOf<String>()
    val sentContents = mutableListOf<List<Content>>()
    var chatStarted = false
    val chat = mockk<Chat>().apply {
        every { sendMessage(any<String>()) } answers {
            sentMessages.add(firstArg())
            responses[index++]
        }
        every { sendMessage(any<List<Content>>()) } answers {
            sentContents.add(firstArg())
            responses[index++]
        }
    }

    override fun startChat(): Chat {
        chatStarted = true
        return chat
    }
}