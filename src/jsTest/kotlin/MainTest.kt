import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mocks.defaultMock
import mocks.engine
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
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
        val mock = engine {
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
                    OpenRDRUI {
                        attrs.scope = this@runTest
                        attrs.api = Api(mock)
                    }
                }
            }
        }.join()

        eventually {
            renderer.findById(NUMBER_OF_CASES_WAITING_ID).text() shouldBe "3"
        }
    }


}
