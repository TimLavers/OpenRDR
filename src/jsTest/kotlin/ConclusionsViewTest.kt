import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.checkContainer
import kotlin.test.Test


class ConclusionsViewTest {
    private val comment1 = "Go to Bondi now!"
    private val comment2 = "Go to Bronte now!"
    private val sun = Attribute("Sun", 55)

    private fun wrapper(vararg comments: String) = VFC {
        comments.forEachIndexed { index, comment ->
            ConclusionsView {
                interpretation = Interpretation().apply {
                    add(
                        RuleSummary(
                            conclusion = Conclusion(1, comment),
                            conditions = setOf(ContainsText(2, sun, "shining $index"))
                        )
                    )
                }
            }
        }
    }

    @Test
    fun shouldShowConclusions() = runTest {
        checkContainer(wrapper(comment1, comment2)) { container ->
            val treeItems = container.querySelectorAll("[role='treeitem']")
            treeItems.length shouldBe 2
            val conclusion1 = treeItems[0]
            val conclusion2 = treeItems[1]
            conclusion1.textContent shouldBe comment1
            conclusion2.textContent shouldBe comment2
        }
    }
}