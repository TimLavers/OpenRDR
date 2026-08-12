package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.containsText

open class RuleTestBase : ConditionTestBase() {
    private var attributeId = 1000
    private var conditionId = 0
    private val caseId = CaseId(1, "Case1")
    val interpretation = Interpretation(caseId)
    private val textToAttribute = mutableMapOf<String, Attribute>()

    /**
     * The comment attribute for the given text, created if this is the first
     * time the text has been used. Each comment text has its own attribute,
     * as in a knowledge base built through the chat. See "Phase 2" in
     * documentation/design/repeat_inferencing.md.
     */
    fun commentAttribute(text: String): Attribute =
        textToAttribute.getOrPut(text) {
            Attribute(
                attributeId++,
                "C${textToAttribute.size + 1}",
                AttributeKind.COMMENT
            )
        }

    /**
     * The assignment that gives the comment with the given text.
     */
    fun comment(text: String) = AssignValue(commentAttribute(text), CommentTemplate(text))

    fun createCondition(text: String): Condition {
        return containsText(conditionId++, clinicalNotes, text)
    }

    fun checkInterpretation(interpretation: Interpretation, vararg assignments: AssignValue) {
        assignments.size shouldBe interpretation.assignments().size
        assignments.forEach {
            interpretation.assignments() shouldContain it
        }
    }
}