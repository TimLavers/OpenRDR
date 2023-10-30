import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import kotlinx.coroutines.MainScope
import mocks.config
import mocks.defaultMock
import mocks.mock
import proxy.findById
import proxy.waitForNextPoll
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class MainTest {

    @Test
    fun caseQueueShouldShowOnTheMainWindow() {
        val fc = FC {
            OpenRDRUI {
                scope = MainScope()
                api = Api(defaultMock)
            }
        }
        runReactTest(fc) { container ->
            container.findById(NUMBER_OF_CASES_ID).textContent shouldBe "$CASES 0"
        }
    }

    @Test
    fun caseViewShouldBeInitialisedWithTheCasesFromTheServer() {
        val config = config {
            val caseId1 = CaseId(1, "case 1")
            val caseId2 = CaseId(2, "case 2")
            val caseId3 = CaseId(3, "case 3")
            returnCasesInfo = CasesInfo(
                listOf(
                    caseId1,
                    caseId2,
                    caseId3
                )
            )
            returnCase = createCase(caseId1)
        }
        val fc = FC {
            OpenRDRUI {
                scope = MainScope()
                api = Api(mock(config))
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                findById(NUMBER_OF_CASES_ID).textContent shouldBe "$CASES 3"
            }
        }
    }
}
