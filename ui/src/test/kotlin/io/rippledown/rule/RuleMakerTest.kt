package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
                RuleMaker(allConditions, mockk(relaxed = true))
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
                RuleMaker(allConditions, mockk(relaxed = true))
            }

            //When
            clickAvailableCondition(2)

            //Then
            requireSelectedConditionsToBeDisplayed(listOf(allConditions[2].asText()))
        }
    }

    @Test
    fun `selecting an available condition with text should append it to the list of selected conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allConditions, mockk(relaxed = true))
            }

            //When
            clickAvailableConditionWithText(allConditions[2].asText())

            //Then
            requireSelectedConditionsToBeDisplayed(listOf(allConditions[2].asText()))
        }
    }

    @Test
    fun `should call handler when the done button is clicked`() {
        val handler = mockk<RuleMakerHandler>(relaxed = true)
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allConditions, handler)
            }
            clickAvailableCondition(2)

            //When
            clickFinishRuleButton()

            //Then
            verify { handler.onDone(listOf(allConditions[2])) }
        }
    }

    @Test
    fun `should call handler when conditions are added`() {
        val handler = mockk<RuleMakerHandler>(relaxed = true)
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allConditions, handler)
            }

            //When
            clickAvailableConditions(listOf(allConditions[1].asText(), allConditions[2].asText()))

            //Then
            verify { handler.onUpdateConditions(listOf(allConditions[1], allConditions[2])) }
        }
    }

    @Test
    fun `should call handler when conditions are removed`() {
        val handler = mockk<RuleMakerHandler>(relaxed = true)
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allConditions, handler)
            }
            clickAvailableConditions(allConditions.map { it.asText() })

            //When
            clickSelectedConditions(listOf(allConditions[1].asText(), allConditions[2].asText()))

            //Then
            verify { handler.onUpdateConditions(listOf(allConditions[0], allConditions[3], allConditions[4])) }
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