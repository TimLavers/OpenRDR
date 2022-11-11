import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterpretationViewTest : ReactTestSupport {

    @Test
    fun shouldShowInitialInterpretation() = runTest {
        val text = "Go to Bondi now!"
        val interpretation = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(text)))
        }
        val renderer = render {
            InterpretationView {
                this.interpretation = interpretation
            }
        }
        renderer.requireInterpretation(text)

        // Check that the text area is in a monospaced font.
        renderer.printJSON()
//        val style = window.getComputedStyle(renderer.interpretationArea() as Element)
//        println("style: $style")

    }

    @Test
    fun shouldShowInitialInterpretationIfBlank() = runTest {
        val renderer = render {
            InterpretationView {
                interpretation = Interpretation()
            }
        }
        renderer.requireNoInterpretation()
    }

    @Test
    fun shouldRenderEnteredInterpretation() {
        val enteredText = "And bring your flippers"
        val interpretation = Interpretation()
        val renderer = render {
            InterpretationView {
                this.interpretation = interpretation
            }
        }
        with(renderer) {
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
        val renderer = render {
            InterpretationView {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = defaultInterpretation
            }
        }
        with(renderer) {
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
        val renderer = render {
            InterpretationView {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = Interpretation(caseId = caseId)
            }
        }
        with(renderer) {
            enterInterpretation(enteredText)
            waitForEvents()
            clickSubmitButton()
            waitForEvents()
            //assertion is in the mocked API call
        }
    }
}