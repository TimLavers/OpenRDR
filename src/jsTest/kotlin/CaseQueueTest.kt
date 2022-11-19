import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseQueueTest : ReactTestSupport {

    @Test
    fun shouldNotShowCaseListIfThereAreNoCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(emptyList())
        }
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                CaseQueue {
                    scope = this@runTest
                    api = Api(mock(config))
                }
            }
        }
        renderer.requireCaseListNotToBeShowing()
    }

    @Test
    fun shouldGetNumberOfWaitingCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", "case 1"),
                    CaseId("2", "case 2"),
                    CaseId("3", "case 3")
                )
            )
            returnCase = RDRCase("case 1")
        }

        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                CaseQueue {
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }
        with(renderer) {
            waitForNextPoll()
            requireNumberOfCasesWaiting(3)
        }
    }

    @Test
    fun shouldNotShowCaseViewWhenThereAreNoMoreCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", "case 1"),
                    CaseId("2", "case 2"),
                    CaseId("3", "case 3")
                )
            )
            returnCase = RDRCase("case 1")
        }
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                CaseQueue {
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }
        with(renderer) {
            waitForNextPoll()
            requireNumberOfCasesWaiting(3) //Sanity check
            config.returnCasesInfo = CasesInfo(emptyList())
            waitForNextPoll()
            requireNumberOfCasesWaiting(0)
        }
    }

    @Test
    fun shouldGetCasesWhenInitialised() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", "case 1"),
                    CaseId("2", "case 2"),
                )
            )
        }

        lateinit var renderer: TestRenderer
        //ensure that the useEffectOnce is called
        act {
            renderer = render {
                CaseQueue {
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }
        waitForNextPoll()
        renderer.requireNumberOfCasesWaiting(2)
    }

    @Test
    fun shouldShowCaseList() = runTest {
        val case1 = "case 1"
        val case2 = "case 2"
        val caseIds = listOf(
            CaseId("1", case1),
            CaseId("2", case2),
        )
        val config = config {
            returnCasesInfo = CasesInfo(
                caseIds
            )
            returnCase = RDRCase(case1)
        }
        val renderer = render {
            CaseQueue {
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        waitForNextPoll()
        renderer.requireNamesToBeShowingOnCaseList(case1, case2)
        renderer.requireCaseToBeSelected(case1)

    }

    @Test
    fun shouldShowCaseViewWhenACaseIsSelected() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseIds = listOf(
            CaseId("1", caseName1),
            CaseId("2", caseName2),
        )
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(caseName1)
        }
        val renderer = render {
            CaseQueue {
                api = Api(mock(config))
                scope = this@runTest
            }
        }

        with(renderer) {
            waitForNextPoll()
            waitForEvents()

            requireNamesToBeShowingOnCaseList(caseName1, caseName2) //sanity check

            selectCase(caseName1)
            waitForEvents()
            requireCaseToBeSelected(caseName1) //sanity check
        }
    }

    @Test
    fun shouldShowNoCasesWhenInterpretationIsSubmittedAndThereAreNoMoreCases() = runTest {
        val caseName = "case 1"
        val caseIds = listOf(
            CaseId("1", caseName)
        )
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(caseName)
        }

        val renderer = render {
            CaseQueue {
                api = Api(mock(config))
                scope = this@runTest
            }
        }

        with(renderer) {
            waitForNextPoll()

            requireNamesToBeShowingOnCaseList(caseName) //sanity check
            selectCase(caseName)
            waitForEvents()

            requireCaseToBeSelected(caseName) //sanity check

            //set the mock to return no cases
            config.returnCasesInfo = CasesInfo(emptyList())

            clickSubmitButton()
            waitForEvents()

            waitFor { numberOfCasesWaiting() == 0 }
            requireCaseListNotToBeShowing()
        }
    }

    @Test
    fun shouldShowOneFewerCaseWhenInterpretationIsSubmitted() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", caseName1),
                    CaseId("2", caseName2),
                )
            )
            returnCase = createCase(caseName1)
        }

        lateinit var renderer: TestRenderer
        launch {
            renderer = render {
                CaseQueue {
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }.join()

        with(renderer) {
            waitForNextPoll()

            selectCase(caseName1)
            waitForEvents()
            requireCaseToBeSelected(caseName1)

            //set the mock to return one case
            config.returnCasesInfo = CasesInfo(listOf(CaseId("2", caseName2)))

            clickSubmitButton()
            waitForEvents()
            waitFor { numberOfCasesWaiting() == 1 }

            //check that the case list shows the one remaining case name
            waitForEvents()
            requireNamesToBeShowingOnCaseList(caseName2)
        }
    }

    @Test
    fun shouldSelectTheFirstCaseWhenInterpretationForOtherCaseIsSubmitted() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseName3 = "case 3"
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", caseName1),
                    CaseId("2", caseName2),
                    CaseId("3", caseName3),
                )
            )
            returnCase = createCase(caseName2)
        }

        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                CaseQueue {
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }

        with(renderer) {
            waitForNextPoll()
            selectCase(caseName2)
            waitForEvents()
            requireCaseToBeSelected(caseName2)

            //set the mock to return the other two cases
            config.returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", caseName1),
                    CaseId("3", caseName3)
                )
            )
            config.returnCase = createCase(caseName1)

            clickSubmitButton()
            waitForEvents()

            waitFor { numberOfCasesWaiting() == 2 }

            //check that the case list shows the two remaining case names
            requireNamesToBeShowingOnCaseList(caseName1, caseName3)

            //and that the first one is selected
            requireCaseToBeSelected(caseName1)
        }
    }

    @Test
    fun shouldSelectTheFirstCaseByDefault() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", "case 1"),
                    CaseId("2", "case 2")
                )
            )
            returnCase = RDRCase("case 1")
        }
        val renderer = render {
            CaseQueue {
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(renderer) {
            waitForNextPoll()
            requireNumberOfCasesWaiting(2)
            requireCaseToBeSelected("case 1")
        }
    }
}
