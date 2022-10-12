import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiTest {

    @Test
    fun getCaseTest() = runTest {
        val case = RDRCase("A", mapOf())
        val config = config {
            returnCase = case
            expectedCaseId = "1"
        }
        Api(mock(config)).getCase("1") shouldBe case
    }

    @Test
    fun waitingForCasesInfoTest() = runTest {
        val expected = CasesInfo(
            listOf(
                CaseId("1", "case 1"),
                CaseId("2", "case 2"),
            )
        )
        val config = config {
            returnCasesInfo = expected
        }

        Api(mock(config)).waitingCasesInfo() shouldBe expected
    }

    @Test
    fun saveInterpretationShouldReturnOperationResultTest() = runTest {
        val expectedResult = OperationResult("saved interpretation for case A")
        val interpretation = Interpretation(CaseId("id1", "Case A"), "report proxy.text")
        val config = config {
            expectedInterpretation = interpretation
            returnOperationResult = expectedResult
        }
        Api(mock(config)).saveInterpretation(interpretation) shouldBe expectedResult
    }
}


