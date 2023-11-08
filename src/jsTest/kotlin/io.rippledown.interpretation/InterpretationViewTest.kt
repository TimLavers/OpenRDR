package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import kotlinx.coroutines.test.TestResult
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class InterpretationViewTest {

    @Test
    fun shouldShowInitialInterpretationIfVerifiedTextIsNull(): TestResult {
        val initialText = "Go to Bondi now!"
        val fc = FC {
            InterpretationView {
                text = initialText
            }
        }
        return runReactTest(fc) { container ->
            container.requireInterpretation(initialText)
        }
    }

    @Test
    fun shouldShowBlankInterpretation(): TestResult {
        val fc = FC {
            InterpretationView {
                text = ""
            }
        }
        return runReactTest(fc) { container ->
            container.requireInterpretation("")
        }
    }

    @Test
    fun shouldCallOnInterpretationEdited(): TestResult {
        val enteredText = "And bring your flippers"
        var updatedText: String? = null
        val fc = FC {
            InterpretationView {
                text = ""
                onEdited = { updated ->
                    updatedText = updated
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                updatedText shouldBe null
                enterInterpretation(enteredText)
                waitForEvents(timeout = 2 * DEBOUNCE_WAIT_PERIOD_MILLIS) //get past the debounce period
                updatedText shouldBe enteredText
            }
        }
    }
}