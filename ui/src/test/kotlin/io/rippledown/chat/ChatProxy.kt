package io.rippledown.chat

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

fun ComposeTestRule.requireChatMessagesShowing(expected: List<ChatMessage>) {
    expected.forEachIndexed { idx, message ->
        val expectedLabel = if (expected[idx].isUser) {
            "$USER$idx"
        } else {
            "$BOT$idx"
        }
        onNodeWithContentDescription(expectedLabel).assertTextEquals(message.text)
    }
}

fun ComposeTestRule.enterChatMessage(message: String) {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).performTextInput(message)
    onNodeWithContentDescription(CHAT_SEND).performClick()
}