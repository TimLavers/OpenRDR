import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.ReferenceRange
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.enterInterpretation
import proxy.requireCaseToBeSelected
import proxy.requireInterpretation
import proxy.waitForDebounce
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

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
    fun shouldCallOnCaseEditedWhenInterpretationIsEdited() = runTest {
        val name = "case a "
        var caseEdited = false
        val vfc = VFC {
            CaseView {
                case = createCase(name)
                scope = this@runTest
                api = Api(mock(config {}))
                onCaseEdited = {
                    caseEdited = true
                }
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            val text = "Go to Bondi now!"
            enterInterpretation(text)
            waitForDebounce()
            requireInterpretation(text)
            caseEdited shouldBe true
        }
    }

    @Test
    fun shouldCallOnStartRuleWhenARuleIsStarted() = runTest {
        val name = "case a "
        var editedInterpretation: Interpretation? = null
        val vfc = VFC {
            CaseView {
                case = createCase(name)
                scope = this@runTest
                api = Api(mock(config {}))
                onStartRule = { interpretation ->
                    editedInterpretation = interpretation
                }
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            val text = "Go to Bondi now!"
            enterInterpretation(text)
            waitForDebounce()
            requireInterpretation(text)
            //todo
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