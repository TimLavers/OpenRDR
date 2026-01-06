package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.utils.applicationFor
import org.junit.Rule
import org.junit.Test

class ChangeInterpretationIconTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should call onClick when clicked`() {
        //Given
        val onClick = mockk<OnClick>(relaxed = true)
        with(composeTestRule) {
            setContent {
                ChangeInterpretationIcon(onClick)
            }

            //When
            clickChangeInterpretationButton()

            //Then
            verify { onClick() }
        }
    }
}

fun main() {
    applicationFor {
        ChangeInterpretationIcon(mockk())
    }
}
