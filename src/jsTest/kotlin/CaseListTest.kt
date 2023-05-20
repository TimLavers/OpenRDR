import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.*
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import io.rippledown.model.createCaseWithInterpretation
import io.rippledown.model.diff.*
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.*
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CaseListTest {

    @Test
    fun shouldListCaseNames() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val twoCaseIds = listOf(
            CaseId(id = "1", name = caseA), CaseId(id = "2", name = caseB)
        )
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = createCase(caseA)
        }

        val vfc = VFC {
            CaseList {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }

        checkContainer(vfc) { container ->
            with(container) {
                findAllById(CASE_ID_PREFIX).length shouldBe 2
                val elementA = findById("${CASE_ID_PREFIX}$caseA")
                val elementB = findById("${CASE_ID_PREFIX}$caseB")
                elementA.textContent shouldBe caseA
                elementB.textContent shouldBe caseB
                printJSON()
            }
        }
    }

    @Test
    fun shouldSelectACaseIdWhenCaseNameClicked() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = "1", name = caseA)
        val caseId2 = CaseId(id = "2", name = caseB)
        val caseId3 = CaseId(id = "3", name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        val config = config {
            returnCasesInfo = CasesInfo(threeCaseIds)
            returnCase = createCase(caseA)
        }
        val vfc = VFC {
            CaseList {
                caseIds = threeCaseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        config.returnCase = createCase(caseB)
        checkContainer(vfc) { container ->
            with(container) {
                selectCase(caseB)
                requireCaseToBeSelected(caseB)
            }
        }
    }

    @Test
    fun shouldShowCaseViewForTheFirstCase() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val twoCaseIds = listOf(
            CaseId("1", caseName1), CaseId("2", caseName2)
        )
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = createCase(caseName1)
        }

        val vfc = VFC {
            CaseList {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireCaseToBeSelected(caseName1)
            }
        }
    }

    @Test
    fun shouldShowCaseListForManyCases() = runTest {

        val caseIds = (1..100).map { i ->
            CaseId(id = i.toString(), name = "case $i")
        }

        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase("case 100")
        }

        val vfc = VFC {
            CaseList {
                this.caseIds = caseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                selectCase("case 100")
                requireCaseToBeSelected("case 100")

            }
        }
    }

    @Test
    fun shouldShowConditionSelector() = runTest {
        val caseName = "Bondi"
        val caseIdList = listOf(CaseId(caseName))
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
            name = caseName,
            conclusionTexts = listOf(beachComment, manlyComment, bondiComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseName
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
        }

        val vfc = VFC {
            CaseList {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForEvents()
            requireCaseToBeSelected(caseName)
            //start to build a rule for the Addition
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(4)
            moveMouseOverRow(2)
            waitForEvents()
            requireDoneButtonNotShowing()
            clickBuildIconForRow(2)
            requireDoneButtonShowing()
        }
    }

    @Test
    fun shouldCancelConditionSelector() = runTest {
        val caseName = "Bondi"
        val caseIdList = listOf(CaseId(caseName))
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
            name = caseName,
            conclusionTexts = listOf(beachComment, manlyComment, bondiComment),
            diffs = diffList
        )
        val config = config {
            expectedCaseId = caseName
            returnCasesInfo = CasesInfo(caseIdList)
            returnCase = caseWithInterp
        }

        val vfc = VFC {
            CaseList {
                caseIds = caseIdList
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForEvents()
            requireCaseToBeSelected(caseName)
            //start to build a rule for the Addition
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(4)
            moveMouseOverRow(2)
            waitForEvents()
            clickBuildIconForRow(2)
            requireCancelButtonShowing()
            //cancel the condition selector
            clickCancelButton()
            waitForEvents()
            requireDoneButtonNotShowing()
        }
    }
}
