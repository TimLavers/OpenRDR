package io.rippledown.interpretation

import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.containsText
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test


class ConclusionsViewTest {
    private val comment1 = "Go to Bondi now!"
    private val comment2 = "Go to Bronte now!"
    private val sun = Attribute(1, "Sun")
    private val surf = Attribute(2, "Surf")

    private fun conditionsForConclusion(index: Int) = listOf(
        containsText(1, sun, "is shining $index"),
        containsText(2, surf, "is great $index")
    ).map { it.asText() }

    private fun wrapper(vararg comments: String) = FC {
        val interp = Interpretation()
        comments.forEachIndexed { index, comment ->
            interp.apply {
                add(
                    RuleSummary(
                        conclusion = Conclusion(index, comment),
                        conditionTextsFromRoot = conditionsForConclusion(index)
                    )
                )
            }
        }
        ConclusionsView {
            interpretation = ViewableInterpretation(interp)
        }
    }

    @Test
    fun shouldShowConclusions() {
        val fc = wrapper(comment1, comment2)
        runReactTest(fc) { container ->
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
    fun shouldShowConditions() {
        val fc = wrapper(comment1, comment2)
        runReactTest(fc) { container ->
            with(container) {
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
}
