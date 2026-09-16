package io.rippledown.appbar

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_NAME_ID
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.model.KBInfo
import org.jetbrains.skia.Image
import org.junit.Rule
import java.io.File
import kotlin.test.Test

class ApplicationBarTest {
    @Test
    fun `KB description is available on hover and focus and updates with the selected KB`() {
        // Given
        val selected = mutableStateOf<KBInfo?>(null)
        val description = mutableStateOf<String?>(null)
        composeTestRule.setContent { ApplicationBar(selected.value, description.value) }
        val icon = composeTestRule.onNodeWithContentDescription("Knowledge base description")
        icon.assertDoesNotExist()

        // When
        composeTestRule.runOnIdle {
            selected.value = bondiInfo
        }
        icon.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Loading description..."))
        composeTestRule.runOnIdle { description.value = "Advice about surfing at Bondi." }
        icon.performMouseInput { moveTo(center) }

        // Then
        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithText("Advice about surfing at Bondi.").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Advice about surfing at Bondi.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(KB_NAME_ID).assertHasNoClickAction()

        // When
        composeTestRule.runOnIdle { description.value = "Updated surfing advice." }

        // Then
        composeTestRule.onNodeWithText("Updated surfing advice.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Advice about surfing at Bondi.").assertDoesNotExist()

        // When
        val longDescription = "Advice about surfing, conditions and choosing a beach. ".repeat(8)
        composeTestRule.runOnIdle { description.value = longDescription }
        val layout = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(longDescription)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }

        // Then
        layout.single().lineCount shouldBeGreaterThan 1
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        val screenshot =
            Image.makeFromBitmap(composeTestRule.onNodeWithText(longDescription).captureToImage().asSkiaBitmap())
        File("build/reports/kb-description-tooltip.png").apply { parentFile.mkdirs() }
            .writeBytes(checkNotNull(screenshot.encodeToData()).bytes)

        // When
        icon.performMouseInput { exit() }
        composeTestRule.runOnIdle {
            selected.value = KBInfo("clinic", "Clinic")
            description.value = "  "
        }
        icon.performSemanticsAction(SemanticsActions.RequestFocus)

        // Then
        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithText("No description set. You can add one through chat.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No description set. You can add one through chat.").assertIsDisplayed()
        icon.fetchSemanticsNode().config[SemanticsProperties.Focused] shouldBe true
        composeTestRule.onNodeWithTag(KB_NAME_ID).assertTextEquals("Clinic")

        // When
        composeTestRule.runOnIdle { selected.value = null }

        // Then
        icon.assertDoesNotExist()
        composeTestRule.onNodeWithText("No description set. You can add one through chat.").assertDoesNotExist()
    }

    @get:Rule
    val composeTestRule = createComposeRule()
    private val bondiInfo = KBInfo("Bondi")

    @Test
    fun `KB label is always read only with no menu`() {
        // Given
        val selected = mutableStateOf<KBInfo?>(null)
        composeTestRule.setContent { ApplicationBar(selected.value) }

        // When
        val label = composeTestRule.onNodeWithTag(KB_NAME_ID, useUnmergedTree = true)

        // Then
        label.assertTextEquals(NO_KB_SELECTED).assertHasNoClickAction()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeTestRule.runOnIdle { selected.value = bondiInfo }
        label.assertTextEquals(bondiInfo.name).assertHasNoClickAction()
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeTestRule.runOnIdle { selected.value = null }
        label.assertTextEquals(NO_KB_SELECTED).assertHasNoClickAction()
    }

    @Test
    fun `should show current KB name`() {
        // Given
        composeTestRule.setContent { ApplicationBar(bondiInfo) }

        // When
        val label = composeTestRule.onNodeWithTag(KB_NAME_ID, useUnmergedTree = true)

        // Then
        label.assertTextEquals(bondiInfo.name)
    }

    @Test
    fun semantics() {
        // Given
        composeTestRule.setContent { ApplicationBar(bondiInfo) }

        // When
        val label = composeTestRule.onNodeWithContentDescription(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION)

        // Then
        label.assertExists()
    }
}
