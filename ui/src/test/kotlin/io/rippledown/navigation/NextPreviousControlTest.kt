package io.rippledown.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.cornerstone.clickNext
import io.rippledown.cornerstone.clickPrevious
import io.rippledown.cornerstone.requireIndexAndTotalToBeDisplayed
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NextPreviousControlTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var handler: NextPreviousControlHandler

    @Before
    fun setUp() {
        handler = mockk(relaxed = true)
    }

    @Test
    fun `should show the current index and total`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(0, 1, handler)
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
                NextPreviousControl(1, 3, handler)
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
                NextPreviousControl(1, 3, handler)
            }
            requireIndexAndTotalToBeDisplayed(1, 3)

            //When
            clickPrevious()

            //Then
            verify { handler.previous() }
        }
    }

    @Test
    fun `the next icon should be disabled for the last item`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(2, 3, handler)
            }
            requireIndexAndTotalToBeDisplayed(2, 3)

            //When
            clickNext()

            //Then
            verify(exactly = 0) { handler.next() }
        }
    }

    @Test
    fun `the previous icon should be disabled for the first item`() {
        with(composeTestRule) {
            //Given
            setContent {
                NextPreviousControl(0, 3, handler)
            }
            requireIndexAndTotalToBeDisplayed(0, 3)

            //When
            clickNext()

            //Then
            verify(exactly = 0) { handler.previous() }
        }
    }
}