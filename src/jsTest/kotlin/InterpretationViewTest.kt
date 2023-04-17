import io.rippledown.interpretation.InterpretationView
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock

import proxy.clickSubmitButton
import proxy.enterInterpretation
import proxy.requireInterpretation
import proxy.waitForEvents
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterpretationViewTest {

    @Test
    fun shouldShowInitialInterpretation() = runTest {
        val text = "Go to Bondi now!"
        val interpretation = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(text)))
        }
        val vfc = VFC {
            InterpretationView {
                this.interpretation = interpretation
            }
        }
        createRootFor(vfc).requireInterpretation(text)

    }

    @Test
    fun shouldShowInitialInterpretationIfBlank() = runTest {
        val vfc = VFC {
            InterpretationView {
                interpretation = Interpretation()
            }
        }
        createRootFor(vfc).requireInterpretation("")
    }

    @Test
    fun shouldRenderEnteredInterpretation() = runTest {
        val enteredText = "And bring your flippers"
        val interpretation = Interpretation()
        val vfc = VFC {
            InterpretationView {
                this.interpretation = interpretation
            }
        }
        with(createRootFor(vfc)) {
            enterInterpretation(enteredText)
            requireInterpretation(enteredText)
        }
    }

    @Test
    fun shouldSubmitDefaultInterpretation() = runTest {
        val caseId = CaseId("1", "case A")
        val text = "Go to Bondi now!"
        val defaultInterpretation = Interpretation(caseId = caseId).apply {
            add(RuleSummary(conclusion = Conclusion(text)))
        }

        val config = config {
            expectedInterpretation = Interpretation(caseId, text)
        }
        val vfc = VFC {
            InterpretationView {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = defaultInterpretation
            }
        }
        with(createRootFor(vfc)) {
            clickSubmitButton()
            waitForEvents()
            //assertion is in the mocked API call
        }
    }

    @Test
    fun shouldSubmitEnteredInterpretation() = runTest {
        val caseId = CaseId("1", "case A")
        val enteredText = "And bring your flippers"
        val config = config {
            expectedInterpretation = Interpretation(caseId, enteredText)
        }
        val vfc = VFC {
            InterpretationView {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = Interpretation(caseId = caseId)
            }
        }
        with(createRootFor(vfc)) {
            enterInterpretation(enteredText)
            waitForEvents()
            clickSubmitButton()
            waitForEvents()
            //assertion is in the mocked API call
        }
    }
}