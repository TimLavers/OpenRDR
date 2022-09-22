import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mysticfall.ReactTestSupport
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseListTest : ReactTestSupport {

    @Test
    fun shouldListCaseIds() {
        val caseIds = listOf(
            CaseId(id = "1", name = "a"),
            CaseId(id = "2", name = "b")
        )
        val renderer = render {
            CaseList {
                attrs.caseIds = caseIds
            }
        }
        val caseList = renderer.root.findByType(CaseList)
        caseList.props.caseIds shouldBe caseIds
    }

    @Test
    fun shouldShowNoCaseView() {
        val renderer = render {
            CaseList {
                attrs.caseIds = emptyList()
            }
        }
        val noCaseView = renderer.root.findByType(NoCaseView)
        noCaseView shouldNotBe null
    }

    @Test
    fun shouldShowCaseView() {
        val caseName = "case a"
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf(CaseId(id = "1", name = caseName))
                attrs.currentCase = RDRCase(name = caseName, data = emptyMap())
            }
        }
        val caseView = renderer.root.findByType(CaseView)
        caseView.props.case.name shouldBe caseName
    }

    @Test
    fun shouldFindTheCaseListHeading() {
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf()
            }
        }
        val heading = renderer.findById(CASELIST_ID).text()
        heading shouldBe "Cases "
    }

    @Test
    fun shouldSelectACaseIdWhenCaseNameClicked() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        var selectedCaseName: String? = null
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf(
                    CaseId(id = "1", name = caseA),
                    CaseId(id = "2", name = caseB),
                    CaseId(id = "3", name = caseC)
                )
                attrs.currentCase = RDRCase(name = caseA, data = emptyMap())
                attrs.onCaseSelected = {
                    selectedCaseName = it
                }
            }
        }
        val listItem = renderer.findById("case_list_item_$caseB")

        selectedCaseName shouldBe null
        click(listItem)
        selectedCaseName shouldBe caseB
    }

    @Test
    fun shouldProcessCaseWhenButtonClicked() = runTest {
        val caseIdA = CaseId(id = "case A", name = "case A")
        val caseIdB = CaseId(id = "case B", name = "case B")
        val caseIdC = CaseId(id = "case C", name = "case C")
        var processedCaseId: CaseId? = null

        val renderer = render {
            CaseList {
                attrs.caseIds = listOf(caseIdA, caseIdB, caseIdC)
                attrs.currentCase = RDRCase(name = "case B", data = emptyMap())
                attrs.onCaseProcessed = { interpretation ->
                    processedCaseId = interpretation.caseId
                }
            }
        }

        val button = renderer.findById("send_interpretation_button")
        processedCaseId shouldBe null
        click(button)
        processedCaseId shouldBe caseIdB
    }
}