package navigation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.navigation.INDEX_AND_TOTAL_ID
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.OF
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.cornerstone.clickNext
import io.rippledown.cornerstone.clickPrevious
import io.rippledown.navigation.NextPreviousControl
import io.rippledown.navigation.NextPreviousControlHandler
import org.junit.Rule
import org.junit.Test

class NextPreviousControlTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val handler = mockk<NextPreviousControlHandler>(relaxed = true)

    @Test
    fun `should call handler when next is clicked`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(3, 42, handler)
            }

            //When
            clickNext()

            //Then
            verify { handler.next() }
        }
    }

    @Test
    fun `should call handler when previous is clicked`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(3, 42, handler)
            }

            //When
            clickPrevious()

            //Then
            verify { handler.previous() }
        }
    }

    @Test
    fun `should show index and total`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(3, 42, handler)
            }

            //Then
            //Note the 1-based index for the display
            onNodeWithContentDescription(INDEX_AND_TOTAL_ID).assertTextEquals("4 $OF 42")
        }
    }

    @Test
    fun `next button should be disabled if on the last index`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(41, 42, handler)
            }

            //Then
            onNodeWithContentDescription(NEXT_BUTTON).assertIsNotEnabled()
        }
    }

    @Test
    fun `previous button should be disabled if on the first index`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(0, 42, handler)
            }

            //Then
            onNodeWithContentDescription(PREVIOUS_BUTTON).assertIsNotEnabled()
        }
    }
}