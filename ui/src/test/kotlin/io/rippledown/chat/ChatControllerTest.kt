package io.rippledown.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatControllerTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    lateinit var handler: ChatControllerHandler

    @Before
    fun setUp() {
        handler = mockk()
    }

    @Test
    fun `should show the initial bot message only`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatController(handler = handler)
            }
            // Then
            requireEmptyChatHistory()
        }
    }

    @Test
    fun `should update the chat history with the user response`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatController(handler = handler)
            }
            // When
            val userMessage = "add a comment"
            typeChatMessageAndClickSend(userMessage)

            // Then
            verify { handler.sendUserMessage(userMessage) }

            val expected = listOf(
                UserMessage(userMessage)
            )
            requireChatMessagesShowing(expected)
        }
    }

    @Test
    fun `should update the chat history with the bot response`() {
        val h = object : ChatControllerHandler {
            override fun sendUserMessage(message: String) {}
            override var onBotMessageReceived: (ChatResponse) -> Unit = {}
        }

        with(composeTestRule) {
            // Given
            setContent {
                ChatController(handler = h)
            }
            // When
            val botResponse = "confirm 42?"
            h.onBotMessageReceived(ChatResponse(botResponse))

            // Then
            val expected = listOf(
                BotMessage(botResponse)
            )
            requireChatMessagesShowing(expected)
        }
    }

    @Test
    fun `should not duplicate a consecutive identical bot response`() {
        val h = object : ChatControllerHandler {
            override fun sendUserMessage(message: String) {}
            override var onBotMessageReceived: (ChatResponse) -> Unit = {}
        }

        with(composeTestRule) {
            // Given
            setContent {
                ChatController(handler = h)
            }
            val botResponse = "confirm 42?"
            h.onBotMessageReceived(ChatResponse(botResponse))

            // When
            h.onBotMessageReceived(ChatResponse(botResponse))

            // Then - only one message, not duplicated
            val expected = listOf(
                BotMessage(botResponse)
            )
            requireChatMessagesShowing(expected)
        }
    }

    @Test
    fun `should give an indication to the user if no bot response is received`() {
        val h = object : ChatControllerHandler {
            override fun sendUserMessage(message: String) {}
            override var onBotMessageReceived: (ChatResponse) -> Unit = {}
        }

        with(composeTestRule) {
            // Given
            setContent {
                ChatController(handler = h)
            }
            val userMessage = "add a comment"
            typeChatMessageAndClickSend(userMessage)

            // When
            h.onBotMessageReceived(ChatResponse(""))

            // Then
            val expected = listOf(
                UserMessage(userMessage),
                BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE),

                )
            requireChatMessagesShowing(expected)
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            val handler = object : ChatControllerHandler {
                override fun sendUserMessage(message: String) {
                    runBlocking {
                        println("User message sent: $message")
                        // Simulate a delay for the bot response
                        delay(1000)
                        onBotMessageReceived(ChatResponse("The answer to '$message' is 42"))
                    }
                }

                override var onBotMessageReceived: (ChatResponse) -> Unit = {}
            }
            ChatController(handler = handler)
        }
    }
}

