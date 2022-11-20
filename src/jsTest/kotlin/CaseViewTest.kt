import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import proxy.clickSubmitButton
import proxy.requireCaseToBeSelected
import proxy.requireInterpretation
import proxy.waitForEvents
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseViewTest : ReactTestSupport {

    @Test
    fun shouldShowCaseName() {
        val name = "case a "
        val renderer = render {
            CaseView {
                case = createCase(name)
            }
        }
        renderer.requireCaseToBeSelected(name)
    }

    @Test
    fun shouldShowInterpretation() {
        val text = "Go to Bondi now!"
        val rdrCase = createCase("case a ")
        rdrCase.interpretation.add(RuleSummary(conclusion = Conclusion(text)))
        val renderer = render {
            CaseView {
                case = rdrCase
            }
        }
        renderer.requireInterpretation(text)
    }

    @Test
    fun shouldCallInterpretationSubmitted() = runTest {
        val text = "Go to Bondi now!"
        val caseName = "case a "
        val rdrCase = createCase(caseName)
        rdrCase.interpretation.add(RuleSummary(conclusion = Conclusion(text)))

        val config = config {
            expectedInterpretation = Interpretation(CaseId(caseName, caseName), text = text)
        }
        val renderer = render {
            CaseView {
                scope = this@runTest
                api = Api(mock(config))
                case = rdrCase
            }
        }
        renderer.requireInterpretation(text) //sanity check

        renderer.clickSubmitButton()
        waitForEvents()
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