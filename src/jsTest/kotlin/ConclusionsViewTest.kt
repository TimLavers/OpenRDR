import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConclusionsViewTest : ReactTestSupport {

    @Test
    fun shouldShowOneConclusion() = runTest {
        val text = "Go to Bondi now!"
        val interpretation = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(text)))
        }
        lateinit var renderer: TestRenderer
        renderer = render {
            ConclusionsView {
                this.interpretation = interpretation
            }
        }
        renderer.printJSON()

//        renderer.requireInterpretation(text)

    }
}