package io.rippledown.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.rippledown.constants.chat.CHAT_BOT_INITIAL_MESSAGE
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
            val expected = listOf(
                initialBotMessage,
                UserMessage(userMessage)
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
            ChatController(mockk())
        }
    }
}

