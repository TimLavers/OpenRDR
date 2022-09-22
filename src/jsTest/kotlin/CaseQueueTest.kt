import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mocks.engine
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
        val mock = engine {
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
                attrs.api = Api(mock)
                attrs.scope = this@runTest
            }
        }

        val refreshButton = renderer.findById(REFRESH_BUTTON_ID)
        click(refreshButton)
        renderer.findById(NUMBER_OF_CASES_WAITING_ID).text() shouldBe "3"
    }


    @Test
    fun shouldGetCasesWhenInitialised() = runTest {
        val mock = engine {
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
                        attrs.api = Api(mock)
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
        val mock = engine {
            returnCasesInfo = CasesInfo(
                caseIds
            )
        }
        val renderer = render {
            CaseQueue {
                attrs.api = Api(mock)
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
}
