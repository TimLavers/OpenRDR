import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.ReferenceRange
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.defaultMock
import mysticfall.ReactTestSupport
import proxy.clickSubmitButton
import proxy.requireCaseToBeSelected
import proxy.requireInterpretation
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseViewTest : ReactTestSupport {

    @Test
    fun shouldShowCaseName() {
        val caseName = "case a"
        val renderer = render {
            CaseView {
                attrs.case = RDRCase(name = caseName)
            }
        }
        renderer.requireCaseToBeSelected(caseName)
    }

    @Test
    fun shouldShowInterpretation() {
        val caseName = "case a"
        val text = "Go to Bondi now!"
        val rdrCase = RDRCase(name = caseName).apply {
            interpretation.add(RuleSummary(conclusion = Conclusion(text)))
        }
        val renderer = render {
            CaseView {
                attrs.case = rdrCase
            }
        }
        renderer.requireInterpretation(text)
    }

    @Test
    fun shouldCallInterpretationSubmitted() = runTest {
        val caseName = "case a"
        val text = "Go to Bondi now!"
        val rdrCase = RDRCase(name = caseName).apply {
            interpretation.add(RuleSummary(conclusion = Conclusion(text)))
        }
        var interpSubmitted = false
        val renderer = render {
            CaseView {
                attrs.scope = this@runTest
                attrs.api = Api(defaultMock)
                attrs.case = rdrCase
                attrs.onInterpretationSubmitted = { interpSubmitted = true }
            }
        }
        interpSubmitted shouldBe false
        renderer.clickSubmitButton()
        interpSubmitted shouldBe true
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
        rangeText(ReferenceRange(null, "2")) shouldBe "(> 2)"
    }

    @Test
    fun shouldFormatOneSidedHighRange() {
        rangeText(ReferenceRange("1", null)) shouldBe "(< 1)"
    }
}


