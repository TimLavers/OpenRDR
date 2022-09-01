import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mysticfall.ReactTestSupport
import mysticfall.TestInstance
import mysticfall.TestRenderer
import kotlin.test.Test


@OptIn(ExperimentalCoroutinesApi::class)
class CaseQueueTest : ReactTestSupport {

    @Test
    fun shouldNotShowCaseListByDefault() {
        val renderer = render {
            CaseQueue()
        }
        renderer.root.findAllByType(CaseList) shouldBe emptyList<TestInstance<*>>()
    }

    @Test
    fun reviewCasesButtonShouldBeInitiallyDisabled() {
        val renderer = render {
            CaseQueue()
        }
        val reviewButton = renderer.findById(REVIEW_CASES_BUTTON_ID)
        reviewButton.props.asDynamic()["disabled"].unsafeCast<Boolean>() shouldBe true
    }

    @Test
    fun shouldGetCasesWhenRefreshButtonIsClicked() = runTest {
        val casesInfo = CasesInfo(
            listOf(
                CaseId("1", "case 1"),
                CaseId("2", "case 2"),
                CaseId("3", "case 3")
            ),
            "some resource path"
        )

        val renderer = render {
            CaseQueue {
                attrs.getWaitingCasesInfo = {
                    casesInfo
                }
            }
        }

        val refreshButton = renderer.findById(REFRESH_BUTTON_ID)
        click(refreshButton)
        renderer.findById(NUMBER_OF_CASES_WAITING_ID).text() shouldBe "3"
    }

    @Test
    fun shouldGetCasesWhenInitialised() = runTest {
        val casesInfo = CasesInfo(
            listOf(
                CaseId("1", "case 1"),
                CaseId("2", "case 2"),
            ),
            "some resource path"
        )
        lateinit var renderer: TestRenderer
        launch {
            //wait for the coroutine in useEffectOnce to finish
            withContext(Dispatchers.Main) {
                //ensure that the useEffectOnce is called
                act {
                    renderer = render {
                        CaseQueue {
                            attrs.getWaitingCasesInfo = {
                                casesInfo
                            }
                        }
                    }
                }
            }
        }.join()
        renderer.findById(NUMBER_OF_CASES_WAITING_ID).text() shouldBe "${casesInfo.count}"
    }

    @Test
    fun shouldShowCaseListWhenReviewButtonClicked() = runTest {
        val caseIds = listOf(
            CaseId("1", "case 1"),
            CaseId("2", "case 2"),
        )
        val casesInfo = CasesInfo(
            caseIds,
            "some resource path"
        )
        val renderer = render {
            CaseQueue {
                attrs.getWaitingCasesInfo = {
                    casesInfo
                }
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
}


