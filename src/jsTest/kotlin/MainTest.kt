import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.defaultMock
import mocks.mock
import proxy.findById
import proxy.waitForNextPoll
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainTest {

    @Test
    fun caseQueueShouldShowOnTheMainWindow() = runTest {
        val vfc = VFC {
            OpenRDRUI {
                scope = this@runTest
                api = Api(defaultMock)
            }
        }
        with(createRootFor(vfc)) {
            findById(NUMBER_OF_CASES_WAITING_ID).textContent shouldBe "Cases waiting: 0"
        }
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
        val vfc = VFC {
            OpenRDRUI {
                scope = this@runTest
                api = Api(mock(config))
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            findById(NUMBER_OF_CASES_WAITING_ID).textContent shouldBe "Cases waiting: 3"
        }
    }
}
