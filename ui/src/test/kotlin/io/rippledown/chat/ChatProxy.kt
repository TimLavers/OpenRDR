@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.chat

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.kotest.assertions.withClue
import io.rippledown.appbar.CHAT_ICON_TOGGLE

fun ComposeTestRule.requireChatMessagesShowing(expected: List<ChatMessage>) {
    expected.forEachIndexed { idx, message ->
        val expectedLabel = if (expected[idx].isUser) {
            "$USER$idx"
        } else {
            "$BOT$idx"
        }
        onNodeWithContentDescription(expectedLabel).assertTextEquals(message.text)
    }
    //And no more messages should be showing
    val size = expected.size
    val unwantedUserLabel = "$USER$size"
    val unwantedBotLabel = "$BOT$size"
    withClue("should be no more user or bot messages") {
        onNodeWithContentDescription(unwantedBotLabel).assertDoesNotExist()
        onNodeWithContentDescription(unwantedUserLabel).assertDoesNotExist()
    }

}

fun ComposeTestRule.requireEmptyChatHistory() {
    val firstBotLabel = "${BOT}0"
    val firstUserMessage = "${USER}0"
    onNodeWithContentDescription(firstBotLabel).assertDoesNotExist()
    onNodeWithContentDescription(firstUserMessage).assertDoesNotExist()
}
fun ComposeTestRule.requireChatPanelIsNotDisplayed() {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).assertDoesNotExist()
}

fun ComposeTestRule.requireChatPanelIsDisplayed() {
    onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsDisplayed()
}

fun ComposeTestRule.clickChatIconToggle() {
    onNodeWithContentDescription(CHAT_ICON_TOGGLE).performClick()
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
