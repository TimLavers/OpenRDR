package io.rippledown.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.chat.KnowledgeBaseListing
import org.jetbrains.skia.Image
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class KbChoiceRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `grouped list shows each name once and opens only available rows`() {
        // Given
        val listing = KnowledgeBaseListing(
            listOf("Biochemistry", "Haematology", "Lipids", "Thyroids"),
            listOf("Contact Lens Prescription", "Pathology", "Thyroid Stimulating Hormone", "Zoo Animals"),
            "Biochemistry"
        )
        val chosen = mutableListOf<String>()
        composeTestRule.setContent {
            Box(Modifier.width(320.dp)) {
                KbChoiceRow(listing, 0) { chosen.add(it) }
            }
        }

        // When
        val lipids = composeTestRule.onNodeWithContentDescription("${KB_CHOICE_ITEM}Lipids")
        lipids.performClick()
        val zoo = composeTestRule.onNodeWithContentDescription("${KB_CHOICE_ITEM}Zoo Animals")
        zoo.performSemanticsAction(SemanticsActions.RequestFocus)
        zoo.performKeyInput { pressKey(Key.Enter) }

        // Then
        chosen shouldBe listOf("Lipids", "Zoo Animals")
        composeTestRule.onNodeWithText("Your knowledge bases")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeTestRule.onNodeWithText("Demonstration knowledge bases")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeTestRule.onNodeWithContentDescription("Your knowledge bases help").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Demonstration knowledge bases help").assertIsDisplayed()
        (listing.storedNames + listing.demonstrationNames).forEach {
            composeTestRule.onAllNodesWithText(it).assertCountEquals(1)
        }
        composeTestRule.onNodeWithContentDescription("${KB_CHOICE_ITEM}Biochemistry").assertIsNotEnabled()
        composeTestRule.onNodeWithText("(current)").assertIsDisplayed()
        val first =
            composeTestRule.onNodeWithContentDescription("${KB_CHOICE_ITEM}Pathology").fetchSemanticsNode().boundsInRoot
        val last = zoo.fetchSemanticsNode().boundsInRoot
        last.top shouldBeGreaterThan first.bottom
        last.left shouldBe first.left
        first.height shouldBeLessThan 25f
        composeTestRule.onNodeWithContentDescription("Your knowledge bases help").performMouseInput { enter() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Click a knowledge base to open it.").assertExists()
        val screenshot = Image.makeFromBitmap(composeTestRule.onRoot().captureToImage().asSkiaBitmap())
        File("build/reports/kb-list.png").apply { parentFile.mkdirs() }
            .writeBytes(requireNotNull(screenshot.encodeToData()).bytes)
    }

    @Test
    fun `empty stored section still offers demonstrations`() {
        // Given
        val listing = KnowledgeBaseListing(emptyList(), listOf("Zoo Animals"))
        var chosen: String? = null
        composeTestRule.setContent { KbChoiceRow(listing, 0) { chosen = it } }

        // When
        composeTestRule.onNodeWithContentDescription("${KB_CHOICE_ITEM}Zoo Animals").performClick()

        // Then
        chosen shouldBe "Zoo Animals"
        composeTestRule.onNodeWithText("Your knowledge bases").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have no knowledge bases of your own.").assertIsDisplayed()
    }

    @Test
    fun `rows are disabled while chat is busy`() {
        // Given
        val listing = KnowledgeBaseListing(listOf("Lipids"), listOf("Pathology"))
        composeTestRule.setContent { KbChoiceRow(listing, 0, enabled = false) }

        // When
        val stored = composeTestRule.onNodeWithContentDescription("${KB_CHOICE_ITEM}Lipids")
        val demo = composeTestRule.onNodeWithContentDescription("${KB_CHOICE_ITEM}Pathology")

        // Then
        stored.assertIsNotEnabled()
        demo.assertIsNotEnabled()
    }
}
