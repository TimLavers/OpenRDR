package io.rippledown.cornerstone

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.cornerstone.EXEMPT_BUTTON
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CornerstoneControlTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var handler: CornerstoneControlHandler

    @Before
    fun setUp() {
        handler = mockk()
    }

    @Test
    fun `should show the current index and total`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstoneControl(0, 1, handler)
            }

            //Then
            requireIndexAndTotalToBeDisplayed(0, 1)
        }
    }

    @Test
    fun `clicking next should call the handler`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstoneControl(1, 3, handler)
            }
            requireIndexAndTotalToBeDisplayed(1, 3)

            //When
            clickNext()

            //Then
            verify { handler.next() }
        }
    }

    @Test
    fun `clicking previous should call the handler`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstoneControl(1, 3, handler)
            }
            requireIndexAndTotalToBeDisplayed(1, 3)

            //When
            clickPrevious()

            //Then
            verify { handler.previous() }
        }
    }

    @Test
    fun `clicking exempt should call the handler`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstoneControl(0, 3, handler)
            }

            //When
            clickExempt()

            //Then
            verify { handler.exempt() }
        }
    }

    @Test
    fun `next button should be disabled if on the last index`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstoneControl(41, 42, handler)
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
                CornerstoneControl(0, 42, handler)
            }

            //Then
            onNodeWithContentDescription(PREVIOUS_BUTTON).assertIsNotEnabled()
        }
    }
    @Test
    fun `exempt button should be disabled if there are no cornerstones`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstoneControl(-1, 0, handler)
            }

            //Then
            onNodeWithContentDescription(EXEMPT_BUTTON).assertIsNotEnabled()
        }
    }
}