package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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
                ConditionFilter(filter, false, handler = mockk())
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
                ConditionFilter("", false, handler = handler)
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
                ConditionFilter("", true, handler = handler)
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
                ConditionFilter("", false, handler = handler)
            }

            //Then
            requireWaitingIndicatorNotToBeShowing()
        }
    }

    @Test
    fun `should show label to enter or select a condition if there is no error`() {
        with(composeTestRule) {
            //Given
            val handler = mockk<ConditionFilterHandler>(relaxed = true)

            //When
            setContent {
                ConditionFilter("", false, handler = handler)
            }

            //Then
            requireEnterConditionMessageToBeShowing()
        }
    }

    @Test
    fun `should show a warning if the expression cannot be parsed`() {
        with(composeTestRule) {
            //Given
            val handler = mockk<ConditionFilterHandler>(relaxed = true)
            val message = DOES_NOT_CORRESPOND_TO_A_CONDITION

            //When
            setContent {
                ConditionFilter("", false, invalidExpression = message, handler = handler)
            }

            //Then
            requireUnknownExpressionMessageToBeShowing()
        }
    }

    @Test
    fun `should show a condition-must-be-true message if the condition is not true for the current case`() {
        with(composeTestRule) {
            //Given
            val handler = mockk<ConditionFilterHandler>(relaxed = true)
            val message = CONDITION_IS_NOT_TRUE

            //When
            setContent {
                ConditionFilter("", false, invalidExpression = message, handler = handler)
            }

            //Then
            requireConditionIsNotTrueMessageToBeShowing()
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ConditionFilter(
                "unrecognizable condition",
                showWaitingIndicator = false,
                invalidExpression = CONDITION_IS_NOT_TRUE,
                handler = mockk(relaxed = true)
            )
        }
    }
}
