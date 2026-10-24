package io.rippledown.chat

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import io.kotest.matchers.shouldBe
import io.rippledown.constants.chat.NOTICE_PREFIX
import io.rippledown.model.UserId
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class ChatPanelNoticeTest {
    @Test
    fun `a notice is a distinct italic row and does not replace the latest bot response`() = runComposeUiTest {
        // Given
        val user = UserId("notice-panel")
        val text = "Bob: Added a rule."
        val layout = mutableListOf<TextLayoutResult>()

        // When
        setContent { ChatPanel(messages = listOf(BotMessage("Welcome."), NoticeMessage(text)), userId = user) }

        // Then
        onNodeWithContentDescription("${NOTICE_PREFIX}1:$text").assertIsDisplayed().assertTextEquals(text)
            .assertHasNoClickAction()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        layout.single().layoutInput.style.fontStyle shouldBe FontStyle.Italic
        onNodeWithContentDescription("${BOT}1:$text").assertDoesNotExist()
        ChatTestHook.snapshot(user).mostRecentBotText shouldBe "Welcome."
    }
}
