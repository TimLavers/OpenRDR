import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
    fun shouldGetCasesWhenInitialised() = runTest {
        val casesInfo = CasesInfo(
            listOf(
                CaseId("1", "case 1"),
                CaseId("2", "case 2"),
            ),
            "some resource path"
        )
        lateinit var renderer: TestRenderer
        renderer = render {
            CaseQueue {
                attrs.getWaitingCasesInfo = {
                    casesInfo
                }
            }
        }
        //Todo
        renderer.findById(NUMBER_OF_CASES_WAITING_ID).text() shouldBe "2"
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
}


