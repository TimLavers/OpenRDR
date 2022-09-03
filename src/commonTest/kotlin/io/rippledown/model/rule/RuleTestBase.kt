package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.ContainsText

internal open class RuleTestBase: ConditionTestBase() {
    val caseId = CaseId("Case1", "Case1")
    val interpretation = Interpretation(caseId, "")

    fun conc(text: String): Conclusion {
        return Conclusion(text)
    }

    fun cond(text: String): Condition {
        return ContainsText(clinicalNotes, text)
    }

    fun checkInterpretation(interpretation: Interpretation, vararg conclusions: Conclusion) {
        conclusions.size shouldBe interpretation.conclusions().size
        conclusions.forEach {
            interpretation.conclusions() shouldContain it
        }
    }

}