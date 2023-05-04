import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.InterpretationView
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.ReferenceRange
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.clickSubmitButton
import proxy.requireCaseToBeSelected
import proxy.requireInterpretation
import proxy.waitForEvents
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseViewTest {

    @Test
    fun shouldShowCaseName() = runTest {
        val name = "case a "
        val vfc = VFC {
            CaseView {
                case = createCase(name)
                scope = this@runTest
                api = Api(mock(config {}))
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireCaseToBeSelected(name)
            }
        }
    }

    @Test
    fun shouldShowInterpretation() = runTest {
        val text = "Go to Bondi now!"
        val rdrCase = createCase("case a ")
        rdrCase.interpretation.add(RuleSummary(conclusion = Conclusion(text)))
        val vfc = VFC {
            CaseView {
                case = rdrCase
                scope = this@runTest
                api = Api(mock(config {}))
            }
        }
        createRootFor(vfc).requireInterpretation(text)
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
    fun shouldCallInterpretationSubmitted() = runTest {
        val caseName = "case A"
        val text = "Go to Bondi now!"
        val rdrCase = createCase(caseName)
        rdrCase.interpretation.add(RuleSummary(conclusion = Conclusion(text)))

        val config = config {
            expectedInterpretation = Interpretation(caseId = CaseId(caseName, caseName), verifiedText = text)
        }
        val vfc = VFC {
            CaseView {
                scope = this@runTest
                api = Api(mock(config))
                case = rdrCase
            }
        }
        with(createRootFor(vfc)) {
            requireInterpretation(text)
            clickSubmitButton()
            waitForEvents()
            //assertion is in the mocked API call
        }
    }

    @Test
    fun shouldFormatNullRange() {
        rangeText(null) shouldBe ""
    }

    @Test
    fun shouldFormatTwoSidedRange() {
        rangeText(ReferenceRange("1", "2")) shouldBe "(1 - 2)"
    }

    @Test
    fun shouldFormatOneSidedLowRange() {
        rangeText(ReferenceRange("1", null)) shouldBe "(> 1)"
    }

    @Test
    fun shouldFormatOneSidedHighRange() {
        rangeText(ReferenceRange(null, "2")) shouldBe "(< 2)"
    }
}