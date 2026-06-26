@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.chat

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.kotest.assertions.withClue

fun ComposeTestRule.requireChatMessagesShowing(expected: List<ChatMessage>) {
    expected.forEachIndexed { idx, message ->
        val expectedLabel = when {
            message.isUser -> "$USER$idx"
            // BotRow now encodes the visible bot text into its own
            // contentDescription as "$BOT$idx:$text" so that integration
            // tests can substring-match against it without recursing into
            // descendants. SuggestionListMessage's outer row keeps the
            // original "$BOT$idx" form because it has no text of its own.
            message is SuggestionListMessage -> "$BOT$idx"
            // TipRow encodes its visible text as "$TIP$idx:$text", mirroring BotRow.
            message is TipMessage -> "$TIP$idx:${message.text}"
            else -> "$BOT$idx:${message.text}"
        }
        onNodeWithContentDescription(expectedLabel).assertTextEquals(message.text)
    }
    //And no more messages should be showing
    val size = expected.size
    val unwantedUserLabel = "$USER$size"
    withClue("should be no more user or bot messages") {
        // Any bot or suggestion row at index `size` would yield an
        // accessibility node whose description begins with "$BOT$size" —
        // either exactly (SuggestionList) or with a ":text" suffix (bot
        // message). Asserting on that prefix via substring-match covers
        // both cases in one check.
        onAllNodesWithContentDescription("$BOT$size", substring = true).assertCountEquals(0)
        onNodeWithContentDescription(unwantedUserLabel).assertDoesNotExist()
    }

}

fun ComposeTestRule.requireEmptyChatHistory() {
    val firstBotLabel = "${BOT}0"
    val firstUserMessage = "${USER}0"
    onNodeWithContentDescription(firstBotLabel).assertDoesNotExist()
    onNodeWithContentDescription(firstUserMessage).assertDoesNotExist()
}

fun ComposeTestRule.requireChatPanelIsDisplayed() {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsDisplayed()
}


fun ComposeTestRule.requireUserMessageShowing(index: Int) {
    val expectedLabel = "$USER$index"
    onNodeWithContentDescription(expectedLabel).assertIsDisplayed()
}

fun ComposeTestRule.deleteChatMessage() {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).performTextClearance()
}

fun ComposeTestRule.performTextInput(text: String) {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).performTextInput(text)
}

fun ComposeTestRule.typeChatMessageAndClickSend(message: String) {
    performTextInput(message)
    onNodeWithContentDescription(CHAT_SEND).performClick()
}

fun ComposeTestRule.typeChatMessageAndPressEnter(message: String) {
    performTextInput(message)
    pressEnter()
}

fun ComposeTestRule.pressEnter() {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).performKeyInput {
        keyDown(Key.Enter)
        keyUp(Key.Enter)
    }

}

fun ComposeTestRule.requireSendButtonDisabled() {
    onNodeWithContentDescription(CHAT_SEND).assertIsNotEnabled()
}

fun ComposeTestRule.requireSendButtonEnabled() {
    onNodeWithContentDescription(CHAT_SEND).assertIsEnabled()
}

fun ComposeTestRule.requireUserTextFieldFocused() {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsFocused()
}

fun ComposeTestRule.requireTypingIndicatorShowing() {
    onNodeWithContentDescription(TYPING_INDICATOR).assertIsDisplayed()
}

fun ComposeTestRule.requireTypingIndicatorNotShowing() {
    onNodeWithContentDescription(TYPING_INDICATOR).assertDoesNotExist()
}
