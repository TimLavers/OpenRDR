package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import kotlinx.coroutines.test.runTest
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class InterpretationViewTest {

    @Test
    fun shouldShowInitialInterpretationIfVerifiedTextIsNull() = runTest {
        val initialText = "Go to Bondi now!"
        val fc = FC {
            InterpretationView {
                text = initialText
            }
        }
        createRootFor(fc).requireInterpretation(initialText)
    }

    @Test
    fun shouldShowBlankInterpretation() = runTest {
        val fc = FC {
            InterpretationView {
                text = ""
            }
        }
        createRootFor(fc).requireInterpretation("")
    }

    @Test
    fun shouldCallOnInterpretationEdited() = runTest {
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
        with(createRootFor(fc)) {
            updatedText shouldBe null
            enterInterpretation(enteredText)
            waitForEvents(timeout = 2 * DEBOUNCE_WAIT_PERIOD_MILLIS) //get past the debounce period
            updatedText shouldBe enteredText
        }
    }
}