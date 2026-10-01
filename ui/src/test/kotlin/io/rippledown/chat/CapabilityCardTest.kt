package io.rippledown.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.rippledown.model.chat.CapabilitySection
import org.jetbrains.skia.Image
import org.junit.Rule
import org.junit.Test
import java.io.File

class CapabilityCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `capability card has compact styled headings and scrolls independently above the input`() {
        // Given
        val sections = listOf(
            CapabilitySection("Knowledge bases", listOf("Import a KB", "Export a KB")),
            CapabilitySection("Report comments", (1..20).map { "Report operation $it" }),
            CapabilitySection("User-defined case lists", listOf("Copy the current case to a named list"))
        )
        compose.setContent {
            Box(Modifier.size(360.dp, 500.dp)) {
                ChatPanel(messages = listOf(CapabilityListMessage("Help", sections)))
            }
        }

        // When
        val heading = compose.onNodeWithText("Knowledge bases", useUnmergedTree = true)
        val first = compose.onNodeWithText("Import a KB", useUnmergedTree = true)
        val layout = mutableListOf<TextLayoutResult>()
        heading.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }

        // Then
        heading.assertIsDisplayed().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        layout.single().layoutInput.style.fontWeight shouldBe FontWeight.SemiBold
        layout.single().layoutInput.style.color shouldBe Color.DarkGray
        val gap = first.fetchSemanticsNode().boundsInRoot.top - heading.fetchSemanticsNode().boundsInRoot.bottom
        gap shouldBeLessThanOrEqualTo with(compose.density) { 4.dp.toPx() }
        compose.onNodeWithTag("CAPABILITY_CARD", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
            .shouldBeLessThanOrEqualTo(with(compose.density) { 380.dp.toPx() })
        compose.onNodeWithTag("CAPABILITY_SCROLLBAR", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsDisplayed()

        val topScreenshot = Image.makeFromBitmap(compose.onRoot().captureToImage().asSkiaBitmap())
        File("build/reports/capability-card-top.png").apply { parentFile.mkdirs() }
            .writeBytes(checkNotNull(topScreenshot.encodeToData()).bytes)

        // When
        compose.onNodeWithText("Copy the current case to a named list", useUnmergedTree = true).performScrollTo()

        // Then
        compose.onNodeWithText("Copy the current case to a named list", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsDisplayed()
        val screenshot = Image.makeFromBitmap(compose.onRoot().captureToImage().asSkiaBitmap())
        File("build/reports/capability-card.png").apply { parentFile.mkdirs() }
            .writeBytes(checkNotNull(screenshot.encodeToData()).bytes)
    }
}
