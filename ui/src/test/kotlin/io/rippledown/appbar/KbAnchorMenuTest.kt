package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.model.KBInfo
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class KbAnchorMenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val handler = mockk<AppBarHandler>()
    private val bondiInfo = KBInfo("bondi_id", "Bondi")

    @Test
    fun `the KB menu offers only import and export`() {
        // Given
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        // When
        composeTestRule.clickDropdown()

        // Then
        with(composeTestRule) {
            onNodeWithText(IMPORT_KB_TEXT).assertIsDisplayed().assertIsEnabled()
            onNodeWithText(EXPORT_KB_TEXT).assertIsDisplayed().assertIsEnabled()
            onNodeWithText(EDIT_KB_DESCRIPTION_BUTTON_TEXT).assertDoesNotExist()
            onNodeWithText(CREATE_KB_TEXT).assertDoesNotExist()
            onNodeWithText(CREATE_KB_FROM_SAMPLE_TEXT).assertDoesNotExist()
            onNodeWithText(SWITCH_KB_HEADER_TEXT).assertDoesNotExist()
            onAllNodes(hasClickAction() and hasAnyAncestor(hasContentDescription(KBS_DROPDOWN_DESCRIPTION)))
                .assertCountEquals(2)
            onNodeWithTag(KB_NAME_ID, useUnmergedTree = true).assertTextEquals(bondiInfo.name)
        }
    }

    @Test
    fun `should show current KB name in trigger when a KB is selected`() {
        // Given
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        // When
        val trigger = composeTestRule.onNodeWithTag(KB_NAME_ID, useUnmergedTree = true)

        // Then
        trigger.assertTextEquals(bondiInfo.name)
    }

    @Test
    fun `should show placeholder and file actions when no KB is selected`() {
        // Given
        composeTestRule.setContent { KbAnchorMenu(null, handler) }

        // When
        composeTestRule.clickDropdown()

        // Then
        with(composeTestRule) {
            onNodeWithTag(KB_NAME_ID, useUnmergedTree = true).assertTextEquals(NO_KB_SELECTED)
            onNodeWithText(IMPORT_KB_TEXT).assertIsEnabled()
            onNodeWithText(EXPORT_KB_TEXT).assertIsEnabled()
        }
    }

    @Test
    fun `should not show the dropdown before the trigger is clicked`() {
        // Given
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        // When
        composeTestRule.waitForIdle()

        // Then
        with(composeTestRule) {
            onNodeWithText(IMPORT_KB_TEXT).assertDoesNotExist()
            onNodeWithText(EXPORT_KB_TEXT).assertDoesNotExist()
        }
    }

    @Test
    fun `should expose the trigger via its content description for accessibility`() {
        // Given
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        // When
        val trigger = composeTestRule.onNodeWithContentDescription(KB_CONTROL_DROPDOWN_DESCRIPTION)

        // Then
        trigger.assertIsDisplayed()
    }
}
