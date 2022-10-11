import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import proxy.clickSubmitButton
import proxy.enterInterpretation
import proxy.requireInterpretation
import proxy.requireNoInterpretation
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
                attrs.interpretation = interpretation
            }
        }
        renderer.requireInterpretation(text)
    }

    @Test
    fun shouldShowInitialInterpretationIfBlank() = runTest {
        val renderer = render {
            InterpretationView {
                attrs.interpretation = Interpretation()
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
                attrs.interpretation = interpretation
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
        var called = false
        val renderer = render {
            InterpretationView {
                attrs.scope = this@runTest
                attrs.api = Api(mock(config))
                attrs.interpretation = defaultInterpretation
                attrs.onInterpretationSubmitted = { called = true }
            }
        }
        with(renderer) {
            clickSubmitButton()
            called shouldBe true
        }
    }

    @Test
    fun shouldSubmitEnteredInterpretation() = runTest {
        val caseId = CaseId("1", "case A")
        val enteredText = "And bring your flippers"
        val config = config {
            expectedInterpretation = Interpretation(caseId, enteredText)
        }
        var called = false
        val renderer = render {
            InterpretationView {
                attrs.scope = this@runTest
                attrs.api = Api(mock(config))
                attrs.interpretation = Interpretation(caseId = caseId)
                attrs.onInterpretationSubmitted = { called = true }
            }
        }
        with(renderer) {
            enterInterpretation(enteredText)
            clickSubmitButton()
            called shouldBe true
        }
    }


}