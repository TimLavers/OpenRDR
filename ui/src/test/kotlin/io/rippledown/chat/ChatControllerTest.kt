package io.rippledown.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.chat.CHAT_BOT_INITIAL_MESSAGE
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatControllerTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    val initialBotMessage = BotMessage(CHAT_BOT_INITIAL_MESSAGE)
    lateinit var handler: ChatControllerHandler

    @Before
    fun setUp() {
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should show the initial bot message only`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatController(handler)
            }
            // Then
            requireChatMessagesShowing(listOf(initialBotMessage))
        }
    }

    @Test
    fun `should update the chat history with the user response`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatController(handler)
            }
            // When
            val userMessage = "add a comment"
            typeChatMessageAndClickSend(userMessage)

            // Then
            verify { handler.sendUserMessage(userMessage) }

            val expected = listOf(
                initialBotMessage,
                UserMessage(userMessage)
            )
            requireChatMessagesShowing(expected)
        }
    }

    @Test
    fun `should update the chat history with the bot response`() {
        val h = object : ChatControllerHandler {
            override fun sendUserMessage(message: String) {}
            override var onBotMessageReceived: (String) -> Unit = {}
        }

        with(composeTestRule) {
            // Given
            setContent {
                ChatController(h)
            }
            // When
            val botResponse = "confirm 42?"
            h.onBotMessageReceived(botResponse)

            // Then
            val expected = listOf(
                initialBotMessage,
                BotMessage(botResponse)
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
                        onBotMessageReceived("The answer to '$message' is 42")
                    }
                }

                override var onBotMessageReceived: (String) -> Unit = {}
            }
            ChatController(handler)
        }
    }
}

