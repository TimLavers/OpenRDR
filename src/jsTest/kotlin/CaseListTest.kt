import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import mysticfall.ReactTestSupport
import kotlin.test.Test

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
        val ids = renderer.root.findAllByType(CaseList).map { c -> c.props.caseIds }
        ids shouldBe listOf(caseIds)
    }

    @Test
    fun shouldShowNoCaseView() {
        val renderer = render {
            CaseList {
                attrs.caseIds = emptyList()
            }
        }
        val noCaseView = renderer.root.findAllByType(NoCaseView)
        noCaseView.size shouldBe 1
    }

    @Test
    fun shouldShowCaseView() {
        val caseName = "case a"
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf(CaseId(id = "1", name = caseName))
                attrs.currentCase = RDRCase(name = caseName)
            }
        }
        val caseViews = renderer.root.findAllByType(CaseView)
        caseViews.size shouldBe 1

        caseViews[0].props.case.name shouldBe caseName
    }

    @Test
    fun shouldFindTheCaseListHeading() {
        val renderer = render {
            CaseList {
                attrs.caseIds = listOf<CaseId>()
            }
        }
        val headingDiv = renderer.root.findAllByType("div")
            .first {
                it.props.asDynamic()["id"] == CASELIST_ID
            }
        val heading = headingDiv.props.asDynamic()["children"][0] as String
        heading shouldBe "Cases "
    }
}
