package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.containsText

open class RuleTestBase: ConditionTestBase() {
    private var conclusionId = 0
    private var conditionId = 0
    private val caseId = CaseId(1, "Case1")
    val interpretation = Interpretation(caseId)

    fun findOrCreateConclusion(text: String, root: Rule): Conclusion {
        var conclusionWithText: Conclusion? = null
        root.visit {
            if (it.conclusion?.text == text) {
                conclusionWithText = it.conclusion
            }
        }
        return conclusionWithText ?: Conclusion(conclusionId++, text)
    }

    fun createCondition(text: String): Condition {
        return containsText(conditionId++, clinicalNotes, text)
    }

    fun checkInterpretation(interpretation: Interpretation, vararg conclusions: Conclusion) {
        conclusions.size shouldBe interpretation.conclusions().size
        conclusions.forEach {
            interpretation.conclusions() shouldContain it
        }
    }
}