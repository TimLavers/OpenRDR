package io.rippledown.interpretation

import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import proxy.waitForEvents
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test


class ConclusionsViewTest {
    private val comment1 = "Go to Bondi now!"
    private val comment2 = "Go to Bronte now!"
    private val sun = Attribute("Sun")
    private val surf = Attribute("Surf")

    private fun conditionsForConclusion(index: Int) = listOf(
        ContainsText(sun, "is shining $index"),
        ContainsText(surf, "is great $index")
    ).map { it.asText() }

    private fun wrapper(vararg comments: String) = VFC {
        val interp = Interpretation()
        comments.forEachIndexed { index, comment ->
            interp.apply {
                add(
                    RuleSummary(
                        conclusion = Conclusion(comment),
                        conditionTextsFromRoot = conditionsForConclusion(index)
                    )
                )
            }
        }
        ConclusionsView {
            interpretation = interp
        }
    }

    @Test
    fun shouldShowConclusions() = runTest {
        checkContainer(wrapper(comment1, comment2)) { container ->
            with(container) {
                requireTreeItemCount(2)
                requireTreeItems(
                    comment1,
                    comment2
                )
            }
        }
    }

    @Test
    fun shouldShowConditions() = runTest {
        with(createRootFor(wrapper(comment1, comment2))) {
            clickComment(comment1)
            waitForEvents()
            requireTreeItemCount(4)
            requireTreeItems(
                comment1,
                "Sun contains \"is shining 0\"",
                "Surf contains \"is great 0\"",
                comment2
            )
            clickComment(comment2)
            requireTreeItemCount(6)
            requireTreeItems(
                comment1,
                "Sun contains \"is shining 0\"",
                "Surf contains \"is great 0\"",
                comment2,
                "Sun contains \"is shining 1\"",
                "Surf contains \"is great 1\"",
            )
        }
    }
}
