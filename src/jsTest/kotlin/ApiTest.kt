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
        val case = createCase("A")
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

    @Test
    fun moveAttributeJustBelowOther() = runTest {
        val expectedResult = OperationResult("Attribute moved.")
        val moved =Attribute("A")
        val target =Attribute("B")
        val config = config {
            returnOperationResult = expectedResult
            expectedMovedAttribute = moved
            expectedTargetAttribute = target
        }
        Api(mock(config)).moveAttributeJustBelowOther(moved, target) shouldBe expectedResult
    }

    @Test
    fun kbInfo() = runTest {
        val expectedResult = KBInfo("Glucose")
        Api(mock(config{})).kbInfo() shouldBe expectedResult
    }
}
