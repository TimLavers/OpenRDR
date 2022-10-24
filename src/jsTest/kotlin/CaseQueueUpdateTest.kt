import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.StringSpec
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

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

        with(renderer) {
            this@runTest.testScheduler.advanceUntilIdle()
            requireNumberOfCasesWaiting(3)

//            eventually(5.seconds ) { requireNumberOfCasesWaiting(3) } //sanity check
            debug("passed sanity check")

            config.returnCasesInfo = CasesInfo(emptyList())
            debug("set returnCasesInfo to empty list")
            eventually(5.seconds ) { requireNumberOfCasesWaiting(0) }
        }
    }

}
