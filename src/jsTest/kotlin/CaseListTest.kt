import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseListTest : ReactTestSupport {

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
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                CaseList {
                    caseIds = threeCaseIds
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }
        config.returnCase = createCase(caseB)
        with(renderer) {
            selectCase(caseB)
            waitForEvents()
            requireCaseToBeSelected(caseB)
        }
    }

    @Test
    fun shouldListCaseNames() = runTest {
        val twoCaseIds = listOf(
            CaseId(id = "1", name = "a"),
            CaseId(id = "2", name = "b")
        )
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = createCase("a")
        }
        val renderer = render {
            CaseList {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = this@runTest

            }
        }
        waitForEvents()
        renderer.requireNamesToBeShowingOnCaseList("a", "b")
    }

    @Test
    fun shouldShowCaseViewForTheFirstCase() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val twoCaseIds = listOf(
            CaseId("1", caseName1),
            CaseId("2", caseName2)
        )
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = createCase(caseName1)
        }

        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                CaseList {
                    caseIds = twoCaseIds
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }
        waitForEvents()
        renderer.requireCaseToBeSelected(caseName1)
    }

    @Test
    fun shouldShowCaseListForManyCases() = runTest {
        lateinit var renderer: TestRenderer

        val caseIds = (1..100).map { i ->
            CaseId(id = i.toString(), name = "case $i")
        }

        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase("case 1")
        }
        act {
            renderer = render {
                CaseList {
                    this.caseIds = caseIds
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }
        waitForEvents()
        config.expectedCaseId = "100"
        renderer.selectCase("case 100")
        //assertion is in the mock
    }
}