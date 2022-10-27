import io.kotest.matchers.shouldNotBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.defaultMock
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.requireNumberOfCasesWaiting
import proxy.waitForNextPoll
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainTest : ReactTestSupport {

    @Test
    fun caseQueueShouldShowOnTheMainWindow() = runTest {
        val renderer = render {
            OpenRDRUI {
                attrs.scope = this@runTest
                attrs.api = Api(defaultMock)
            }
        }
        renderer.root.findAllByType(CaseQueue) shouldNotBe null
    }

    @Test
    fun caseViewShouldBeInitialisedWithTheWaitingCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId("1", "case 1"),
                    CaseId("2", "case 2"),
                    CaseId("3", "case 3")
                )
            )
        }
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                OpenRDRUI {
                    attrs.scope = this@runTest
                    attrs.api = Api(mock(config))
                }
            }
        }
        waitForNextPoll()
        renderer.requireNumberOfCasesWaiting(3)
    }


}
