package io.rippledown.persistence

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.rule.*
import kotlin.test.Test

internal class DerivedDefinitionSerializationTest {
    private val notes = Attribute(3, "clinical_notes")
    private val hasDiabetes = AiBoolean(
        sourceAttribute = notes,
        instructions = "True if the notes affirm a personal history of diabetes.",
        examples = listOf(AiBooleanExample("FH diab", false, "Family history only"), AiBooleanExample("T2 Diab", true)),
        revision = "rev-1",
        evaluationProfile = AiEvaluationProfile("gemini", "gemini-2.5-flash", 0.0, 1, 1)
    )

    @Test
    fun `an AI Boolean round trips through the stored form`() {
        expressionFromString(expressionToString(hasDiabetes)) shouldBe hasDiabetes
    }

    @Test
    fun `definitions stored before AI Booleans existed still read`() {
        // Given the stored forms of a formula, a literal, a comment template and the sentinel, as persisted today
        val formula = """{"type":"Formula","expression":{"type":"Binary","operator":"DIVIDE","left":{"type":"AttributeValue","attribute":{"id":1,"name":"weight"}},"right":{"type":"AttributeValue","attribute":{"id":2,"name":"height"}}}}"""
        val literal = """{"type":"Literal","value":"diabetic"}"""
        val template = """{"type":"CommentTemplate","text":"BMI is ${'$'}{}.","variables":[{"id":10,"name":"BMI","kind":"DERIVED"}]}"""
        val byDefinition = """{"type":"ByDefinition"}"""

        // When they are read
        // Then each is the expression it was
        expressionFromString(formula) shouldBe
                Formula(Binary(Operator.DIVIDE, AttributeValue(Attribute(1, "weight")), AttributeValue(Attribute(2, "height"))))
        expressionFromString(literal) shouldBe Literal("diabetic")
        expressionFromString(template) shouldBe CommentTemplate("BMI is \${}.", listOf(Attribute(10, "BMI")))
        expressionFromString(byDefinition) shouldBe ByDefinition
    }
}
