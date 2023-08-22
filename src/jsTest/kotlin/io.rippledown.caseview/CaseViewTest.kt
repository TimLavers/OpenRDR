package io.rippledown.caseview

import Api
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.*
import io.rippledown.model.*
import io.rippledown.model.diff.*
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CaseViewTest {

    @Test
    fun shouldShowCaseName() = runTest {
        val name = "case a "
        val fc = FC {
            CaseView {
                case = createCase(name)
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                requireCaseToBeShowing(name)
            }
        }
    }

    @Test
    fun shouldCallOnStartRuleWithExpectedInterpretation() = runTest {
        val bondiComment = "Go to Bondi now!"
        val manlyComment = "Go to Manly now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(
            listOf(
                Unchanged(beachComment),
                Removal(manlyComment),
                Addition(bondiComment),
                Replacement(manlyComment, bondiComment)
            )
        )
        val caseWithInterp = createCaseWithInterpretation(
            id = 1L,
            conclusionTexts = listOf(beachComment, manlyComment, bondiComment),
            diffs = diffList
        )

        val config = config {
            returnInterpretation = Interpretation(
                diffList = diffList,
            )
        }
        lateinit var diff: Diff
        val fc = FC {
            CaseView {
                case = caseWithInterp
                scope = this@runTest
                api = Api(mock(config))
                onCaseEdited = {}
                onStartRule = { selectedDiff ->
                    diff = selectedDiff
                }
            }
        }
        with(createRootFor(fc)) {
            { "sanity check" }.asClue {
                requireBadgeCount(3)
            }

            //start to build a rule for the Addition
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(4)
            moveMouseOverRow(2)
            waitForEvents()
            clickBuildIconForRow(2)
            diff shouldBe Addition(bondiComment)
        }
    }
}

@Test
fun shouldShowInterpretation() = runTest {
    val text = "Go to Bondi now!"
    val rdrCase = createCase("case a")
    rdrCase.interpretation.add(RuleSummary(conclusion = Conclusion(1, text)))
    val fc = FC {
        CaseView {
            case = rdrCase
            scope = this@runTest
            api = Api(mock(config {}))
        }
    }
    createRootFor(fc).requireInterpretation(text)
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