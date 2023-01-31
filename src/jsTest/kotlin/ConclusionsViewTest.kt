import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.RuleSummary
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import react.VFC
import react.dom.test.runReactTest
import kotlin.test.Test


@OptIn(ExperimentalCoroutinesApi::class)
class ConclusionsViewTest {
    private val comment1 = "Go to Bondi now!"
    private val comment2 = "Go to Bronte now!"
    private val sun = Attribute("Sun")


    private fun wrapper(vararg comments: String) = VFC {
        comments.forEachIndexed { index, comment ->
            ConclusionsView {
                interpretation = Interpretation().apply {
                    add(
                        RuleSummary(
                            conclusion = Conclusion(comment),
                            conditions = setOf(ContainsText(sun, "shining $index"))
                        )
                    )
                }
            }
        }
    }

    @Test
    fun shouldShowConclusions() = runReactTest(wrapper(comment1, comment2)) { container ->
        val treeItems = container.querySelectorAll("[role='treeitem']")
        treeItems.length shouldBe 2
        val conclusion1 = treeItems[0]
        val conclusion2 = treeItems[1]
        conclusion1.textContent shouldBe comment1
        conclusion2.textContent shouldBe comment2
    }
}