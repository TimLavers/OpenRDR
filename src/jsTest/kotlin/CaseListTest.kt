import api.Api
import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import mysticfall.ReactTestSupport
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
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
        val headingDiv = renderer.root.findAllByType(div.toString())
            .first {
                it.props.asDynamic()["id"] == CASELIST_ID
            }
        val heading = headingDiv.props.asDynamic()["children"][0] as String
        heading shouldBe "Cases "
    }

    @Test
    fun shouldSelectACaseIdWhenCaseNameClicked() {
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
                attrs.currentCase = RDRCase(name = caseA)
                attrs.onCaseSelected = {
                    selectedCaseName = it
                }
            }
        }
        val listItem = renderer.root.findAllByType(li.toString())
            .first {
                it.props.asDynamic()["id"] == "case_list_item_$caseB"
            }

        selectedCaseName shouldBe null

        listItem.props.asDynamic().onClick()

        selectedCaseName shouldBe caseB
    }

    @Test
    fun shouldProcessCaseWhenButtonClicked() {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        var caseWasProcessed = false

        class ApiMock : Api {
            override fun interpretationSubmitted(interpretation: Interpretation) {
            }
        }

        val renderer = render {
            CaseList {
                attrs.caseIds = listOf(
                    CaseId(id = "1", name = caseA),
                    CaseId(id = "2", name = caseB),
                    CaseId(id = "3", name = caseC)
                )
                attrs.currentCase = RDRCase(name = caseA)
                attrs.onCaseProcessed = {
                    caseWasProcessed = true
                }
                attrs.api = ApiMock()
            }
        }

        val button = renderer.root.findAllByType(button.toString())
            .first {
                it.props.asDynamic()["id"] == "send_interpretation_button"
            }

        caseWasProcessed shouldBe false

        button.props.asDynamic().onClick()

        caseWasProcessed shouldBe true
    }

}


