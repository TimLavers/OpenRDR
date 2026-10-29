package io.rippledown.chat

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.chat.NOTICE_PREFIX
import io.rippledown.model.UserId
import org.junit.jupiter.api.Test
import org.jetbrains.skia.Image
import java.io.File

@OptIn(ExperimentalTestApi::class)
class ChatPanelNoticeTest {
    @Test
    fun `a notice uses a white rounded bubble with grey italic text and remains distinct from a bot reply`() = runComposeUiTest {
        // Given
        val user = UserId("notice-panel")
        val text = "Alice: Added comment \"Comment 4.\" with no conditions."
        val layout = mutableListOf<TextLayoutResult>()

        // When
        setContent {
            ChatPanel(
                messages = listOf(BotMessage("Welcome."), NoticeMessage(text)),
                userId = user,
                modifier = Modifier.width(300.dp).height(260.dp)
            )
        }

        // Then
        val notice = onNodeWithContentDescription("${NOTICE_PREFIX}1:$text")
        notice.assertIsDisplayed().assertTextEquals(text)
            .assertHasNoClickAction()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        layout.single().layoutInput.style.fontStyle shouldBe FontStyle.Italic
        layout.single().layoutInput.style.fontSize shouldBe 12.sp
        layout.single().layoutInput.style.color shouldBe Color.Black.copy(alpha = 0.65f)
        layout.single().lineCount shouldBeGreaterThan 1
        val pixels = notice.captureToImage().toPixelMap()
        pixels[pixels.width / 2, 2] shouldBe Color.White
        pixels[0, 0] shouldNotBe Color.White
        notice.fetchSemanticsNode().boundsInRoot.left shouldBe
                onNodeWithContentDescription("${BOT}0:Welcome.").fetchSemanticsNode().boundsInRoot.left
        onNodeWithContentDescription("${BOT}1:$text").assertDoesNotExist()
        ChatTestHook.snapshot(user).mostRecentBotText shouldBe "Welcome."

        val screenshot = Image.makeFromBitmap(
            onNodeWithContentDescription("${NUMBER_OF_CHAT_MESSAGES_}2").captureToImage().asSkiaBitmap()
        )
        File("build/reports/chat-notice-bubble.png").apply { parentFile.mkdirs() }
            .writeBytes(checkNotNull(screenshot.encodeToData()).bytes)
    }
}
