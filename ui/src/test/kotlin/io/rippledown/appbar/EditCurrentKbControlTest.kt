package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.main.EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION
import org.junit.Before
import org.junit.Rule
import javax.swing.SwingUtilities.invokeAndWait
import kotlin.test.Test

class EditCurrentKbControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val bondiDescription = "An iconic white sanded beach with a good surf in most conditions"

    private lateinit var handler: KbEditControlHandler

    @Before
    fun setup() {
        handler = mockk<KbEditControlHandler>()
        every { handler.kbDescription } returns { bondiDescription }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `KB description menu`() {
        with(composeTestRule) {
            setContent {
                EditCurrentKbControl(handler)
            }
            assertKbDescriptionMenuItemIsNotShowing()
            clickEditKbDropdown()

            assertEditKbDescriptionMenuItemIsShowing()
            clickKbDescriptionMenuItem()
            assertKbDescriptionOkButtonIsEnabled()
            onNodeWithContentDescription(EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION)
                .assertIsEnabled()
                .assertIsDisplayed()
            waitUntilExactlyOneExists(hasText(bondiDescription))

            assertKbDescriptionMenuItemIsNotShowing()
        }
    }

    @Test
    fun `set the kb description`() {
        with(composeTestRule) {
            setContent {
                EditCurrentKbControl(handler)
            }
            clickEditKbDropdown()
            clickKbDescriptionMenuItem()
            val newDescription = "900 metres of littoral perfection with a south-easterly aspect"
            enterKbDescription(newDescription)

            invokeAndWait { clickConfirmDescriptionButton() }

            verify { handler.setKbDescription(newDescription) }
        }
    }
}