package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test


class ConditionFilterTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should show the text entered into the filter`() {
        with(composeTestRule) {
            //Given
            val filter = "Waves are high"
            setContent {
                ConditionFilter(filter, false, mockk())
            }

            //Then
            requireConditionFilterText(filter)
        }
    }

    @Test
    fun `should call the handler when text is entered into the filter`() {
        with(composeTestRule) {
            //Given
            val filter = "Waves are high"
            val handler = mockk<ConditionFilterHandler>(relaxed = true)
            setContent {
                ConditionFilter("", false, handler)
            }

            //When
            enterTextIntoConditionFilter(filter)

            //Then
            verify { handler.onFilterChange(filter) }
        }
    }

    @Test
    fun `should show the waiting indicator if specified`() {
        with(composeTestRule) {
            //Given
            val handler = mockk<ConditionFilterHandler>(relaxed = true)
            setContent {
                ConditionFilter("", true, handler)
            }

            //Then
            requireWaitingIndicatorToBeShowing()
        }
    }

    @Test
    fun `should not show the waiting indicator if it is not specified`() {
        with(composeTestRule) {
            //Given
            val handler = mockk<ConditionFilterHandler>(relaxed = true)
            setContent {
                ConditionFilter("", false, handler)
            }

            //Then
            requireWaitingIndicatorNotToBeShowing()
        }
    }
}


