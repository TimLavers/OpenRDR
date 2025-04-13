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
                ChatMessage("a", false),
                ChatMessage("b", true),
                ChatMessage("c", false),
                ChatMessage("d", true)
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
            enterChatMessage(userMessage)

            // Then
            val expected = ChatMessage(userMessage, true)
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
                    ChatMessage("?", false),
                    ChatMessage("!", true)
                ), mockk()
            )
        }
    }
}

