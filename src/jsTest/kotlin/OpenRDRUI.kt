import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.constants.main.MAIN_HEADING_ID
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import main.Api
import main.OpenRDRUI
import mocks.config
import mocks.defaultMock
import mocks.mock
import proxy.findById
import proxy.requireNumberOfCasesNotToBeShowing
import proxy.waitForNextPoll
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class OpenRDRUITest {

    @Test
    fun shouldShowHeading(): TestResult {
        val fc = FC {
            OpenRDRUI {
                scope = MainScope()
                api = Api(defaultMock)
            }
        }
        return runReactTest(fc) { container ->
            container.findById(MAIN_HEADING_ID).textContent shouldBe MAIN_HEADING
        }
    }

    @Test
    fun shouldNotShowEmptyCaseQueueTest(): TestResult {
        val fc = FC {
            OpenRDRUI {
                scope = MainScope()
                api = Api(defaultMock)
            }
        }
        return runReactTest(fc) { container ->
            container.requireNumberOfCasesNotToBeShowing()
        }
    }

    @Test
    fun caseViewShouldBeInitialisedWithTheCasesFromTheServer(): TestResult {
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
        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                findById(NUMBER_OF_CASES_ID).textContent shouldBe "$CASES 3"
            }
        }
    }
}
