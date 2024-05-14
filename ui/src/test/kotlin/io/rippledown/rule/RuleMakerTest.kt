package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.condition.Condition
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleMakerTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    lateinit var allConditions: List<Condition>

    @Before
    fun setUp() {
        allConditions = (1..5).map { index ->
            val condition = mockk<Condition>(relaxed = true)
            every { condition.asText() } returns "Condition $index"
            condition
        }
    }

    @Test
    fun `the available conditions should initially be all the conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allConditions, mockk())
            }

            //Then
            requireAvailableConditionsToBeDisplayed(allConditions.map { it.asText() })
        }
    }

    @Test
    fun `selecting an available condition should append it to the list of selected conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allConditions, mockk())
            }

            //When
            clickAvailableCondition(2)

            //Then
            requireSelectedConditionsToBeDisplayed(listOf(allConditions[2].asText()))
        }
    }

}

fun main() {
    val handler = mockk<RuleMakerHandler>(relaxed = true)
    val conditions = (1..10).map { index ->
        val condition = mockk<Condition>(relaxed = true)
        every { condition.asText() } returns "This is condition $index"
        condition
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            RuleMaker(conditions, handler)
        }
    }
}