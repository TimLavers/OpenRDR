import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.checkContainer
import proxy.*
import react.VFC
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseListReactTest : ReactTestSupport {
    @Test
    fun shouldListCaseNames() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val twoCaseIds = listOf(
            CaseId(id = "1", name = caseA),
            CaseId(id = "2", name = caseB)
        )
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = createCase(caseA)
        }

        val vfc = VFC {
            CaseList {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = this@runTest

            }
        }

        checkContainer(vfc) { container ->
            container.findAllById(CASE_ID_PREFIX).length shouldBe 2
            val elementA = container.findById("${CASE_ID_PREFIX}$caseA")
            val elementB = container.findById("${CASE_ID_PREFIX}$caseB")
            elementA.textContent shouldBe caseA
            elementB.textContent shouldBe caseB
        }
    }


}