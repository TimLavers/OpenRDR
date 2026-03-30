package io.rippledown.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
        onMessageSent = mockk()
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
                ChatPanel(id = 0L, sendIsEnabled = true, messages = messages, onMessageSent)
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
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
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
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
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
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
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
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
            }

            // When
            val userMessage = "add a comment"
            typeChatMessageAndPressEnter(userMessage)

            // Then
            val expected = UserMessage(userMessage)
            verify { onMessageSent.invoke(expected) }
        }
    }

    @Test
    fun `should scroll to the most recent message`() {
        val history = (1..100).map { UserMessage("message $it") }
        val index = history.size - 1
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, history, onMessageSent)
            }

            // Then
            requireUserMessageShowing(index)
        }
    }

    @Test
    fun `should set focus on the user text field`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
            }

            // Then
            requireUserTextFieldFocused()
        }
    }

    @Test
    fun `should set focus on the user text field after clicking the send button`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
            }
            requireUserTextFieldFocused()

            // When
            val userMessage = "add a comment"
            typeChatMessageAndClickSend(userMessage)

            // Then
            requireUserTextFieldFocused()
        }
    }

    @Test
    fun `should set focus on the user text field after pressing the Enter key`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
            }
            requireUserTextFieldFocused()

            // When
            val userMessage = "add a comment"
            typeChatMessageAndPressEnter(userMessage)

            // Then
            requireUserTextFieldFocused()
        }
    }

    @Test
    fun `should set focus on the user text field when the recomposed with a different id`() {
        with(composeTestRule) {
            // Given
            setContent {
                ParentComposable()
            }
            requireUserTextFieldFocused()

            // When
            onNodeWithContentDescription("TEST_BUTTON").performClick()

            // Then
            requireUserTextFieldFocused()
        }
    }

    @Test
    fun `should show typing indicator when sendIsEnabled is false`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = false, listOf(), onMessageSent)
            }

            // Then
            requireTypingIndicatorShowing()
        }
    }

    @Test
    fun `should not show typing indicator when sendIsEnabled is true`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, listOf(), onMessageSent)
            }

            // Then
            requireTypingIndicatorNotShowing()
        }
    }

    @Test
    fun `should show typing indicator with existing messages when sendIsEnabled is false`() {
        with(composeTestRule) {
            // Given
            val messages = listOf(
                BotMessage("Hello"),
                UserMessage("Hi there")
            )
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = false, messages, onMessageSent)
            }

            // Then
            requireChatMessagesShowing(messages)
            requireTypingIndicatorShowing()
        }
    }

    @Test
    fun `should hide typing indicator when sendIsEnabled changes from false to true`() {
        with(composeTestRule) {
            // Given
            var sendEnabled by mutableStateOf(false)
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = sendEnabled, listOf(), onMessageSent)
            }
            requireTypingIndicatorShowing()

            // When
            sendEnabled = true
            waitForIdle()

            // Then
            requireTypingIndicatorNotShowing()
        }
    }

    @Test
    fun `should show typing indicator when sendIsEnabled changes from true to false`() {
        with(composeTestRule) {
            // Given
            var sendEnabled by mutableStateOf(true)
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = sendEnabled, listOf(), onMessageSent)
            }
            requireTypingIndicatorNotShowing()

            // When
            sendEnabled = false
            waitForIdle()

            // Then
            requireTypingIndicatorShowing()
        }
    }

    @Test
    fun `should disable text field when typing indicator is showing`() {
        with(composeTestRule) {
            // Given
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = false, listOf(), onMessageSent)
            }

            // Then
            requireTypingIndicatorShowing()
            onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsNotEnabled()
        }
    }

    @Test
    fun `clicking a non-editable suggestion should populate the text field`() {
        with(composeTestRule) {
            // Given
            val messages = listOf<ChatMessage>(
                BotMessage("Here are some suggestions."),
                SuggestionListMessage(listOf("Waves is in case", "Sun is in case"))
            )
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, messages = messages, onMessageSent)
            }

            // When
            onNodeWithText("1. Waves is in case").performClick()

            // Then - text field should contain the suggestion text
            onNodeWithContentDescription(CHAT_TEXT_FIELD).assertTextContains("Waves is in case")
            verify(exactly = 0) { onMessageSent.invoke(any()) }
        }
    }

    @Test
    fun `clicking an editable suggestion should auto-send the message`() {
        with(composeTestRule) {
            // Given
            val messages = listOf<ChatMessage>(
                BotMessage("Here are some suggestions."),
                SuggestionListMessage(listOf("Waves ≥ 1.5 [editable]", "Sun is in case"))
            )
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, messages = messages, onMessageSent)
            }

            // When
            onNodeWithText("1. Waves ≥ 1.5").performClick()

            // Then - message should be sent automatically with [editable] marker
            verify(exactly = 1) { onMessageSent.invoke(UserMessage("Waves ≥ 1.5 [editable]")) }
        }
    }

    @Test
    fun `clicking an editable suggestion should not populate the text field`() {
        with(composeTestRule) {
            // Given
            val messages = listOf<ChatMessage>(
                BotMessage("Here are some suggestions."),
                SuggestionListMessage(listOf("Waves ≥ 1.5 [editable]"))
            )
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, messages = messages, onMessageSent)
            }

            // When
            onNodeWithText("1. Waves ≥ 1.5").performClick()

            // Then - send button should be disabled (text field is empty)
            requireSendButtonDisabled()
        }
    }

    @Test
    fun `clicking an editable suggestion when sendIsEnabled is false should not send`() {
        with(composeTestRule) {
            // Given
            val messages = listOf<ChatMessage>(
                BotMessage("Here are some suggestions."),
                SuggestionListMessage(listOf("Waves ≥ 1.5 [editable]"))
            )
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = false, messages = messages, onMessageSent)
            }

            // When
            onNodeWithText("1. Waves ≥ 1.5").performClick()

            // Then - no message should be sent
            verify(exactly = 0) { onMessageSent.invoke(any()) }
        }
    }

    @Test
    fun `clicking an editable suggestion multiple times should only send once`() {
        with(composeTestRule) {
            // Given
            val messages = listOf<ChatMessage>(
                BotMessage("Here are some suggestions."),
                SuggestionListMessage(listOf("Waves ≥ 1.5 [editable]"))
            )
            setContent {
                ChatPanel(id = 0L, sendIsEnabled = true, messages = messages, onMessageSent)
            }

            // When - click multiple times
            onNodeWithText("1. Waves ≥ 1.5").performClick()
            onNodeWithText("1. Waves ≥ 1.5").performClick()
            onNodeWithText("1. Waves ≥ 1.5").performClick()

            // Then - should only have sent once
            verify(exactly = 1) { onMessageSent.invoke(UserMessage("Waves ≥ 1.5 [editable]")) }
        }
    }
}

// A parent composable to test the behaviour of the ChatPanel when the caseId changes
@Composable
fun ParentComposable() {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var uniqueId by remember { mutableStateOf(0L) }

    Column(verticalArrangement = Arrangement.Top) {
        ChatPanel(
            id = uniqueId,
            sendIsEnabled = true,
            messages = messages,
            onMessageSent = { userMessage ->
                messages = messages + userMessage
            },
            modifier = Modifier.height(400.dp) //leave space for the button
        )
        Button(
            onClick = {
                messages = listOf()
                uniqueId = ++uniqueId
            },
            modifier = Modifier.semantics {
                contentDescription = "TEST_BUTTON"
            }) {
            Text("Click to reset chat")
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ChatPanel(
                id = -1,
                sendIsEnabled = true,
                listOf(
                    BotMessage("Hi there"),
                    UserMessage("Meaning of life?"),
                    BotMessage("42")
                ), mockk()
            )
        }
    }
}