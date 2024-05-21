package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleControlButtonsTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    lateinit var handler: RuleControlButtonsHandler

    @Before
    fun setUp() {
        handler = mockk(relaxed = true)
    }

    @Test
    fun `clicking the finish button should call the handler`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleControlButtons(handler)
            }
            //When
            clickFinishRuleButton()

            //Then
            verify { handler.finish() }
        }
    }

    @Test
    fun `clicking the cancel button should call the handler`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleControlButtons(handler)
            }
            //When
            clickCancelRuleButton()

            //Then
            verify { handler.cancel() }
        }
    }

}