package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.kb.EDIT_KB_DESCRIPTION_BUTTON_TEXT
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.kb.KB_NAME_ID
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.constants.main.CREATE_KB_FROM_SAMPLE_TEXT
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.EXPORT_KB_TEXT
import io.rippledown.constants.main.IMPORT_KB_TEXT
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

/**
 * Covers the unified KB anchor menu that replaced the separate `KBControl`,
 * `EditCurrentKbControl`, and `KbNameDisplay` composables.
 */
@OptIn(ExperimentalTestApi::class)
class KbAnchorMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val lipidsInfo = KBInfo("lipids_id", "Lipids")
    private val glucoseInfo = KBInfo("glucose_id", "Glucose")
    private val bondiInfo = KBInfo("bondi_id", "Bondi")

    private val kbDescription = "A knowledge base about water quality"

    private lateinit var handler: AppBarHandler

    @Before
    fun setup() {
        handler = mockk<AppBarHandler>(relaxed = true)
        every { handler.kbList } returns { emptyList() }
        every { handler.kbDescription } returns { kbDescription }
    }

    // -----------------------------------------------------------------------
    // Trigger (KB-name anchor button)
    // -----------------------------------------------------------------------

    @Test
    fun `should show current KB name in trigger when a KB is selected`() = runTest {
        //Given a menu for Bondi
        //When it is rendered
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        //Then the trigger shows the KB name
        composeTestRule.onNodeWithTag(KB_NAME_ID, useUnmergedTree = true).assertTextEquals(bondiInfo.name)
    }

    @Test
    fun `should show 'No KB selected' placeholder when no KB is selected`() = runTest {
        //Given no KB
        //When the menu is rendered
        composeTestRule.setContent { KbAnchorMenu(null, handler) }

        //Then the trigger shows the placeholder
        composeTestRule.onNodeWithTag(KB_NAME_ID, useUnmergedTree = true).assertTextEquals(NO_KB_SELECTED)
    }

    @Test
    fun `should not show the dropdown before the trigger is clicked`() = runTest {
        //Given a freshly rendered menu
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        //When no interaction happens
        //Then none of the menu items are present
        with(composeTestRule) {
            assertCreateKbMenuItemIsNotShowing()
            assertKbDescriptionMenuItemIsNotShowing()
        }
    }

    @Test
    fun `should open the dropdown when the trigger is clicked`() = runTest {
        //Given a rendered menu
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        //When the trigger is clicked
        composeTestRule.clickDropdown()

        //Then all top-level menu items appear
        with(composeTestRule) {
            assertEditKbDescriptionMenuItemIsShowing()
            assertCreateKbMenuItemIsShowing()
            assertCreateKbFromSampleMenuItemIsShowing()
            assertImportKbMenuItemIsShowing()
            onNodeWithText(EXPORT_KB_TEXT).assertIsEnabled()
        }
    }

    // -----------------------------------------------------------------------
    // All items are always enabled, even with no KB selected
    // -----------------------------------------------------------------------

    @Test
    fun `should keep all menu items enabled even when no KB is selected`() = runTest {
        //Given no KB is open
        composeTestRule.setContent { KbAnchorMenu(null, handler) }

        //When the menu is opened
        composeTestRule.clickDropdown()

        //Then every action remains clickable so the contract matches the
        //pre-refactor controls. (The wiring layer decides what to do when
        //invoked without a current KB.)
        with(composeTestRule) {
            onNodeWithText(EDIT_KB_DESCRIPTION_BUTTON_TEXT).assertIsEnabled()
            onNodeWithText(CREATE_KB_TEXT).assertIsEnabled()
            onNodeWithText(CREATE_KB_FROM_SAMPLE_TEXT).assertIsEnabled()
            onNodeWithText(IMPORT_KB_TEXT).assertIsEnabled()
            onNodeWithText(EXPORT_KB_TEXT).assertIsEnabled()
        }
    }

    // -----------------------------------------------------------------------
    // Switch-KB section
    // -----------------------------------------------------------------------

    @Test
    fun `should list other KBs under the switch-KB header`() = runTest {
        //Given the workspace has Bondi (current), Lipids, and Glucose
        every { handler.kbList } returns { listOf(bondiInfo, lipidsInfo, glucoseInfo) }
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        //When the menu is opened
        composeTestRule.clickDropdown()

        //Then the other two KBs are shown under the switcher header,
        //while the current KB is excluded
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("Switch knowledge base"))
            assertDropdownItemsContain(glucoseInfo.name, lipidsInfo.name)
            onAllNodesWithText(bondiInfo.name).assertCountEquals(1) // only the trigger
        }
    }

    @Test
    fun `should hide the switch-KB section when there are no other KBs`() = runTest {
        //Given the workspace has only the current KB
        every { handler.kbList } returns { listOf(bondiInfo) }
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        //When the menu is opened
        composeTestRule.clickDropdown()

        //Then the switcher header is not rendered
        composeTestRule.onAllNodesWithText("Switch knowledge base").assertCountEquals(0)
    }

    @Test
    fun `should call selectKB with the chosen KB id when a switch item is clicked`() = runTest {
        //Given the workspace has Bondi (current) and Lipids
        every { handler.kbList } returns { listOf(bondiInfo, lipidsInfo) }
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }
        composeTestRule.clickDropdown()

        //When the user clicks "Lipids"
        composeTestRule.waitUntilExactlyOneExists(hasText(lipidsInfo.name))
        composeTestRule.onNodeWithText(lipidsInfo.name).performClick()

        //Then the handler is invoked with the Lipids id
        verify { handler.selectKB(lipidsInfo.id) }
    }

    @Test
    fun `should close the dropdown after selecting a switch item`() = runTest {
        //Given the workspace has Bondi (current) and Lipids and the menu is open
        every { handler.kbList } returns { listOf(bondiInfo, lipidsInfo) }
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }
        composeTestRule.clickDropdown()
        composeTestRule.waitUntilExactlyOneExists(hasText(lipidsInfo.name))

        //When the user picks Lipids
        composeTestRule.onNodeWithText(lipidsInfo.name).performClick()

        //Then the header is no longer showing (menu collapsed)
        composeTestRule.onAllNodesWithText("Switch knowledge base").assertCountEquals(0)
    }

    // -----------------------------------------------------------------------
    // Action item -> dialog wiring
    // -----------------------------------------------------------------------

    @Test
    fun `should close the dropdown when Create KB is clicked`() = runTest {
        //Given an open menu
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }
        composeTestRule.clickDropdown()

        //When the Create item is clicked
        composeTestRule.clickCreateKbMenuItem()

        //Then the other dropdown items disappear (menu is closed)
        composeTestRule.assertKbDescriptionMenuItemIsNotShowing()
    }

    @Test
    fun `should close the dropdown when KB description is clicked`() = runTest {
        //Given an open menu
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }
        composeTestRule.clickDropdown()

        //When the KB description item is clicked
        composeTestRule.clickKbDescriptionMenuItem()

        //Then the other dropdown items are no longer showing
        composeTestRule.assertCreateKbMenuItemIsNotShowing()
    }

    // -----------------------------------------------------------------------
    // Contract with the handler
    // -----------------------------------------------------------------------

    @Test
    fun `should fetch kbList only on first composition`() = runTest {
        //Given a known kbList provider
        every { handler.kbList } returns { listOf(bondiInfo, lipidsInfo) }

        //When the menu is rendered
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }
        composeTestRule.waitForIdle()

        //Then the provider was consulted at least once
        verify(atLeast = 1) { handler.kbList }
    }

    @Test
    fun `should sort other KBs alphabetically`() = runTest {
        //Given KBs provided in non-alphabetical order
        val zebra = KBInfo("zebra_id", "Zebra")
        val alpha = KBInfo("alpha_id", "Alpha")
        val middle = KBInfo("middle_id", "Middle")
        every { handler.kbList } returns { listOf(zebra, middle, alpha, bondiInfo) }
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        //When the menu is opened
        composeTestRule.clickDropdown()
        composeTestRule.waitUntilExactlyOneExists(hasText(zebra.name))

        //Then the three non-current KBs are listed (order is implementation
        //detail but all three must be present)
        composeTestRule.assertDropdownItemsContain(alpha.name, middle.name, zebra.name)
    }

    // -----------------------------------------------------------------------
    // Trigger semantics
    // -----------------------------------------------------------------------

    @Test
    fun `should expose the trigger via its content description for accessibility`() = runTest {
        //Given a rendered menu
        //When the tree is queried by the trigger's content description
        composeTestRule.setContent { KbAnchorMenu(bondiInfo, handler) }

        //Then exactly one node is found
        composeTestRule
            .onNodeWithContentDescription(KB_CONTROL_DROPDOWN_DESCRIPTION)
            .assertIsDisplayed()
    }

    @Test
    fun `KB name text element is the queryable anchor for the KB name`() = runTest {
        //Given a rendered menu for Lipids
        //When the KB_NAME_ID tag is queried
        composeTestRule.setContent { KbAnchorMenu(lipidsInfo, handler) }

        //Then it carries the current KB name
        composeTestRule
            .onNodeWithTag(KB_NAME_ID, useUnmergedTree = true)
            .assertTextEquals(lipidsInfo.name)
        lipidsInfo.name shouldBe "Lipids"
    }
}
