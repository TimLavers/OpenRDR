import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.engine
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiTest {

    @Test
    fun getCaseTest() = runTest {
        val case = RDRCase("A", mapOf())
        val mock = engine {
            returnCase = case
            expectedCaseId = "1"
        }
        Api(mock).getCase("1") shouldBe case
    }

    @Test
    fun waitingForCasesInfoTest() = runTest {
        val expected = CasesInfo(
            listOf(
                CaseId("1", "case 1"),
                CaseId("2", "case 2"),
            )
        )
        val mock = engine {
            returnCasesInfo = expected
        }

        Api(mock).waitingCasesInfo() shouldBe expected
    }

    @Test
    fun saveInterpretationShouldReturnOperationResultTest() = runTest {
        val expectedResult = OperationResult("saved interpretation for case A")
        val interpretation = Interpretation(CaseId("id1", "Case A"), "report text")
        val mock = engine {
            expectedInterpretation = interpretation
            returnOperationResult = expectedResult
        }

        Api(mock).saveInterpretation(interpretation) shouldBe expectedResult
    }
}


