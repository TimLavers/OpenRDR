import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.defaultMock
import mocks.mock
import proxy.findById
import proxy.waitForNextPoll
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

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
            findById(NUMBER_OF_CASES_ID).textContent shouldBe "$CASES 0"
        }
    }

    @Test
    fun caseViewShouldBeInitialisedWithTheCasesFromTheServer() = runTest {
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
        val vfc = VFC {
            OpenRDRUI {
                scope = this@runTest
                api = Api(mock(config))
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            findById(NUMBER_OF_CASES_ID).textContent shouldBe "$CASES 3"
        }
    }
}
