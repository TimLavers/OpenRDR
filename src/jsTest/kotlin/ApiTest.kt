import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.HasCurrentValue
import io.rippledown.model.diff.*
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import kotlin.test.Test

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
        Api(mock(config {})).kbInfo() shouldBe expectedResult
    }

    @Test
    fun theReturnedInterpretationShouldContainTheDiffList() = runTest {
        val expectedDiffList = DiffList(
            listOf(
                Addition("This comment was added."),
                Removal("This comment was removed."),
                Replacement("This comment was replaced.", "This is the new comment."),
                Unchanged("This comment was left alone."),
            )
        )
        val interpretation = Interpretation(CaseId("id1", "Case A"), "report proxy.text")
        val config = config {
            expectedInterpretation = interpretation
            returnInterpretation = interpretation.copy(diffList = expectedDiffList)
        }
        Api(mock(config)).saveVerifiedInterpretation(interpretation) shouldBe interpretation.copy(diffList = expectedDiffList)
    }

    @Test
    fun conditionHints() = runTest {
        val conditionList = ConditionList(
            listOf(
                HasCurrentValue(Attribute("A")),
                HasCurrentValue(Attribute("B"))
            )
        )
        val config = config {
            returnConditionList = conditionList
        }
        Api(mock(config)).conditionHints("any") shouldBe conditionList
    }

}
