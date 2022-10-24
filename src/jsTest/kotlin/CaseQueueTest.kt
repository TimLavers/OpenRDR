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
    fun shouldNotShowCaseListByDefault() = runTest {
        val renderer = render {
            CaseQueue {
                attrs.scope = this@runTest
            }
        }
        renderer.requireCaseListNotToBeShowing()
    }

    @Test
    fun reviewCasesButtonShouldBeInitiallyDisabled() = runTest {
        val renderer = render {
            CaseQueue {
                attrs.scope = this@runTest
            }
        }
        renderer.requireReviewButtonDisabled()
    }


    @Test
    fun shouldGetCasesWhenRefreshButtonIsClicked() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", "case 1"),
                    CaseId("2", "case 2"),
                    CaseId("3", "case 3")
                )
            )
        }
        val renderer = render {
            CaseQueue {
                attrs.api = Api(mock(config))
                attrs.scope = this@runTest
            }
        }
        with(renderer) {
            requireNumberOfCasesWaiting(0)
            clickRefreshButton()
        }
        this@runTest.launch {
            renderer.requireNumberOfCasesWaiting(3)
        }
    }

    @Test
    fun shouldNotShowCaseViewWhenRefreshButtonIsClickedAndThereAreNoMoreCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", "case 1"),
                    CaseId("2", "case 2"),
                    CaseId("3", "case 3")
                )
            )
        }
        val renderer = render {
            CaseQueue {
                attrs.api = Api(mock(config))
                attrs.scope = this@runTest
            }
        }
        with(renderer) {
            clickRefreshButton()
            waitForEvents()
            requireNumberOfCasesWaiting(3) //Sanity check

            config.returnCasesInfo = CasesInfo(emptyList())
            clickRefreshButton()
            waitForEvents()
            requireNumberOfCasesWaiting(0)
            requireNoCaseView()
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
        launch {
            //ensure that the useEffectOnce is called
            act {
                renderer = render {
                    CaseQueue {
                        attrs.api = Api(mock(config))
                        attrs.scope = this@runTest
                    }
                }
            }
        }.join()
        withContext(Dispatchers.Default) {
            delay(2000) // Dispatchers.Default doesn't know about TestCoroutineScheduler
        }
        renderer.printJSON()
        waitForEvents()
        renderer.requireNumberOfCasesWaiting(2)
    }

    @Test
    fun shouldShowCaseListWhenReviewButtonClicked() = runTest {
        val caseIds = listOf(
            CaseId("1", "case 1"),
            CaseId("2", "case 2"),
        )
        val config = config {
            returnCasesInfo = CasesInfo(
                caseIds
            )
        }
        val renderer = render {
            CaseQueue {
                attrs.api = Api(mock(config))
                attrs.scope = this@runTest
            }
        }
        with(renderer) {
            clickRefreshButton() //enable the review button
            clickReviewButton()
        }
        waitForEvents()
        renderer.requireNamesToBeShowingOnCaseList("case 1", "case 2")
        renderer.requireNoCaseView()
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
            returnCase = RDRCase(caseName1)
        }
        val renderer = render {
            CaseQueue {
                attrs.api = Api(mock(config))
                attrs.scope = this@runTest
            }
        }

        with(renderer) {
            clickRefreshButton() //enable the review button
            clickReviewButton() //show the case list
            waitForEvents()

            requireNamesToBeShowingOnCaseList(caseName1, caseName2) //sanity check
            requireNoCaseView()

            selectCase(caseName1)
            waitForEvents()
            requireCaseToBeSelected(caseName1) //sanity check
        }
    }

    @Test
    fun shouldShowNoCaseViewWhenInterpretationIsSubmittedAndThereAreNoMoreCases() = runTest {
        val caseName = "case 1"
        val caseIds = listOf(
            CaseId("1", caseName)
        )
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = RDRCase(caseName)
        }

        val renderer = render {
            CaseQueue {
                attrs.api = Api(mock(config))
                attrs.scope = this@runTest
            }
        }

        with(renderer) {
            clickRefreshButton() //enable the review button
            clickReviewButton() //show the case list
            waitForEvents()

            requireNamesToBeShowingOnCaseList(caseName) //sanity check
            selectCase(caseName)
            waitForEvents()

            requireCaseToBeSelected(caseName) //sanity check

            //set the mock to return no cases
            config.returnCasesInfo = CasesInfo(emptyList())

            clickSubmitButton()
            waitForEvents()

            requireNoCaseView()
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
            returnCase = RDRCase(caseName1)
        }

        lateinit var renderer: TestRenderer
        launch {
            renderer = render {
                CaseQueue {
                    attrs.api = Api(mock(config))
                    attrs.scope = this@runTest
                }
            }
        }.join()

        with(renderer) {
            clickRefreshButton() //enable the review button
            clickReviewButton()  //show the case list and the case
            waitForEvents()

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
            returnCase = RDRCase(caseName2)
        }

        lateinit var renderer: TestRenderer
        launch {
            renderer = render {
                CaseQueue {
                    attrs.api = Api(mock(config))
                    attrs.scope = this@runTest
                }
            }
        }.join()

        with(renderer) {
            clickRefreshButton() //enable the review button
            clickReviewButton()  //show the case list and the case
            waitForEvents()

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
            config.returnCase = RDRCase(caseName1)

            clickSubmitButton()
            waitForEvents()

            waitFor { numberOfCasesWaiting() == 2 }

            //check that the case list shows the two remaining case names
            requireNamesToBeShowingOnCaseList(caseName1, caseName3)

            //and that the first one is selected
            requireCaseToBeSelected(caseName1)
        }
    }
}
