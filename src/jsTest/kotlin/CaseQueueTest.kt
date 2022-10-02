import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestInstance
import mysticfall.TestRenderer
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
        renderer.root.findAllByType(CaseList) shouldBe emptyList<TestInstance<*>>()
    }

    @Test
    fun reviewCasesButtonShouldBeInitiallyDisabled() = runTest {
        val renderer = render {
            CaseQueue {
                attrs.scope = this@runTest
            }
        }
        val reviewButton = renderer.findById(REVIEW_CASES_BUTTON_ID)
        reviewButton.props.asDynamic()["disabled"].unsafeCast<Boolean>() shouldBe true
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

        val refreshButton = renderer.findById(REFRESH_BUTTON_ID)
        click(refreshButton)
        renderer.findById(NUMBER_OF_CASES_WAITING_ID).text() shouldBe "3"
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
        renderer.findById(NUMBER_OF_CASES_WAITING_ID).text() shouldBe "2"
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
        val refreshButton = renderer.findById(REFRESH_BUTTON_ID)
        val reviewButton = renderer.findById(REVIEW_CASES_BUTTON_ID)
        click(refreshButton) //enable the review button

        renderer.root.findAllByType(CaseList) shouldBe emptyList<TestInstance<*>>()
        click(reviewButton)

        val caseList = renderer.root.findByType(CaseList)
        caseList.props.caseIds shouldBe caseIds
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
            expectedCaseId = caseIds[0].id
        }

        lateinit var renderer: TestRenderer
        launch {
            act {
                renderer = render {
                    CaseQueue {
                        attrs.api = Api(mock(config))
                        attrs.scope = this@runTest
                    }
                }
            }
        }.join()
        val refreshButton = renderer.findById(REFRESH_BUTTON_ID)
        val reviewButton = renderer.findById(REVIEW_CASES_BUTTON_ID)
        click(refreshButton) //enable the review button
        click(reviewButton) //show the case list and the case

        val caseList = renderer.root.findByType(CaseList)
        caseList.props.caseIds shouldBe caseIds

        val caseLink = renderer.findById("$CASE_ID_PREFIX$caseName")
        click(caseLink)

        val caseView = renderer.root.findByType(CaseView)
        caseView.props.case.name shouldBe "case 1"

        //set the mock to return no cases
        config.returnCasesInfo = CasesInfo(emptyList())

        val submitButton = renderer.findById(SEND_INTERPRETATION_BUTTON_ID)
        click(submitButton)

        //check that the case list is no longer shown
        renderer.root.findAllByType(CaseList) shouldBe emptyList<TestInstance<*>>()
    }
}
