import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseQueueUpdateTest : ReactTestSupport {
    @Test
    fun shouldPeriodicallyGetCases() = runTest {
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
                CaseQueue {
                    api = Api(mock(config))
                    scope = this@runTest
                }
            }
        }
        with(renderer) {
            waitForNextPoll()
            requireNumberOfCasesWaiting(3) //sanity check that the first poll has completed

            config.returnCasesInfo = CasesInfo(emptyList())
            waitForNextPoll()
            requireNumberOfCasesWaiting(0)
        }
    }

}
