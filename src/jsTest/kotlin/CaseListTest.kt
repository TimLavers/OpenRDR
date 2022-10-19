import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mocks.defaultMock
import mysticfall.ReactTestSupport
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseListTest : ReactTestSupport {

    @Test
    fun shouldListCaseNames() {
        val caseIds = listOf(
            CaseId(id = "1", name = "a"),
            CaseId(id = "2", name = "b")
        )
        val renderer = render {
            CaseList {
                attrs.caseIds = caseIds
            }
        }
        renderer.requireNamesToBeShowingOnCaseList("a", "b")
    }

    @Test
    fun shouldShowNoCaseView() {
        val renderer = render {
            CaseList {
                attrs.caseIds = emptyList()
            }
        }
        renderer.requireNoCaseView()
    }

    @Test
    fun shouldShowCaseViewForTheSelectedCase() {
        val caseName = "case a"
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf(CaseId(id = "1", name = caseName))
                attrs.currentCase = RDRCase(name = caseName)
            }
        }
        renderer.requireCaseToBeSelected(caseName)
    }

    @Test
    fun shouldShowTheCaseListHeading() {
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf()
            }
        }
        renderer.requireCaseListHeading(CASELIST_HEADING)
    }

    @Test
    fun shouldSelectACaseIdWhenCaseNameClicked() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = "1", name = caseA)
        val caseId2 = CaseId(id = "2", name = caseB)
        val caseId3 = CaseId(id = "3", name = caseC)
        var selectedCaseId: String? = null
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf(
                    caseId1,
                    caseId2,
                    caseId3
                )
                attrs.currentCase = RDRCase(name = caseA, data = emptyMap())
                attrs.onCaseSelected = { id ->
                    selectedCaseId = id
                }
            }
        }
        selectedCaseId shouldBe null
        renderer.selectCase(caseB)
        selectedCaseId shouldBe caseId2.id
    }

    @Test
    fun shouldCallInterpretationSubmitted() = runTest {
        val caseName = "case a"
        val text = "Go to Bondi now!"
        val caseId = CaseId(name = caseName)
        val rdrCase = RDRCase(name = caseName).apply {
            interpretation.add(RuleSummary(conclusion = Conclusion(text)))
        }
        var interpSubmitted = false
        val renderer = render {
            CaseList {
                attrs.scope = this@runTest
                attrs.api = Api(defaultMock)
                attrs.caseIds = listOf(caseId)
                attrs.currentCase = rdrCase
                attrs.onInterpretationSubmitted = { interpSubmitted = true }
            }
        }
        renderer.clickSubmitButton()
        this@runTest.launch {
            interpSubmitted shouldBe true
        }
    }
}