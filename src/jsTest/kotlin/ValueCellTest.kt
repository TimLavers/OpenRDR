import io.kotest.matchers.shouldBe
import io.rippledown.model.TestResult
import kotlin.test.Test

class ValueCellTest {

    @Test
    fun shouldShowResultTextWithoutUnits() {
        resultText(TestResult("12.8", null, null)) shouldBe "12.8"
    }

    @Test
    fun shouldShowResultTextWithUnits() {
        resultText(TestResult("12.8", null, "waves / sec")) shouldBe "12.8 waves / sec"
    }
}


