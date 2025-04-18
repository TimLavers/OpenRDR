package io.rippledown.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatPanelTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    lateinit var onMessageSent: OnMessageSent

    @Before
    fun setUp() {
        onMessageSent = mockk(relaxed = true)
    }

    @Test
    fun `should show the chat panel with the chat history`() {
        with(composeTestRule) {
            // Given
            val messages = listOf(
                BotMessage("a"),
                UserMessage("b"),
                BotMessage("c"),
                UserMessage("d")
            )
            // When
            setContent {
                ChatPanel(messages, onMessageSent)
            }
            // Then
            requireChatMessagesShowing(messages)
        }
    }

    @Test
    fun `should call onMessageSent when the user enters a message`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(listOf(), onMessageSent)
            }

            // When
            val userMessage = "add a comment"
            typeChatMessageAndClickSend(userMessage)

            // Then
            val expected = UserMessage(userMessage)
            verify { onMessageSent.invoke(expected) }
        }
    }

    @Test
    fun `should not call onMessageSent if there is no user message to send`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(listOf(), onMessageSent)
            }
            performTextInput("add a comment")
            requireSendButtonEnabled()

            // When deleting the message
            deleteChatMessage()

            // Then
            requireSendButtonDisabled()
        }
    }

    @Test
    fun `should not enable the send button if there is no user text`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(listOf(), onMessageSent)
            }

            // Then
            requireSendButtonDisabled()
        }
    }

    @Test
    fun `should call onMessageSent when the user presses the Enter key`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(listOf(), onMessageSent)
            }

            // When
            val userMessage = "add a comment"
            typeChatMessageAndPressEnter(userMessage)

            // Then
            val expected = UserMessage(userMessage)
            verify { onMessageSent.invoke(expected) }
        }
    }

}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ChatPanel(
                listOf(
                    BotMessage("Hi there"),
                    UserMessage("Meaning of life?"),
                    BotMessage("42")
                ), mockk(relaxed = true)
            )
        }
    }
}

