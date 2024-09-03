package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.model.Conclusion
import io.rippledown.utils.applicationFor
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
    }

    @Test
    fun `should show non-blank interpretation`() = runTest {
        val text = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                InterpretationView(listOf(Conclusion(0, text)))
            }
            requireInterpretation(text)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                InterpretationView(listOf())
            }
            requireInterpretation("")
        }
    }
}

fun main() {
    applicationFor {
        InterpretationView(
            listOf(
                Conclusion(0, "Surf's up!"),
                Conclusion(1, "Go to Bondi now!")
            )
        )
    }
}