package io.rippledown.interpretation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.casecontrol.CaseControl
import io.rippledown.casecontrol.CaseControlHandler
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.mock
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldShowInitialInterpretationIfVerifiedTextIsNull()=runTest {
        val initialText = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                InterpretationView(object : Handler by handlerImpl, InterpretationViewHandler {
                    override var text= initialText
                    override var onEdited = {_ : String -> }
                    override var isCornertone= false
                })
            }

            requireInterpretation(initialText)
        }
    }



    /*
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
     */
}