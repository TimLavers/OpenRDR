package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.utils.defaultDate
import io.rippledown.utils.serializeDeserialize
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class AiBooleanTest {
    private val notes = Attribute(1, "clinical_notes")
    private val profile = AiEvaluationProfile(
        provider = "gemini",
        model = "gemini-2.5-flash",
        temperature = 0.0,
        promptVersion = 1,
        schemaVersion = 1
    )
    private val examples = listOf(
        AiBooleanExample("FH diab", false, "Family history only"),
        AiBooleanExample("T2 Diab", true)
    )
    private val hasDiabetes = AiBoolean(
        sourceAttribute = notes,
        instructions = "True if the notes affirm a personal history of diabetes.",
        examples = examples,
        revision = "rev-1",
        evaluationProfile = profile
    )

    private fun case() = with(RDRCaseBuilder()) {
        addValue(notes, defaultDate, "T2 Diab")
        build("Case")
    }

    @Test
    fun `cannot be evaluated directly`() {
        // Given an AI Boolean definition
        // When it is evaluated against a case without the server's evaluator
        // Then evaluation fails: the classification is obtained through the evaluation boundary
        shouldThrow<IllegalStateException> {
            hasDiabetes.evaluate(case())
        }.message shouldBe "An AiBoolean expression is evaluated through the server's evaluator, not directly."
    }

    @Test
    fun `references its source attribute`() {
        hasDiabetes.referencedAttributes() shouldBe setOf(notes)
    }

    @Test
    fun `text form names the source and the criteria`() {
        hasDiabetes.asText() shouldBe "AI Boolean from clinical_notes: True if the notes affirm a personal history of diabetes."
    }

    @Test
    fun `summary names the source without the criteria`() {
        // Given the full criteria are available in the definition's text form
        // When a compact display of the expression is needed, such as a pending-change cell
        // Then only the kind and the source attribute are named
        hasDiabetes.summary() shouldBe "AI derived attribute from clinical_notes"
    }

    @Test
    fun `alignment replaces the source by id and changes nothing else`() {
        // Given the KB now holds the source under a new name
        val renamed = Attribute(1, "Notes")

        // When the definition is aligned
        val aligned = hasDiabetes.alignAttributes { id ->
            if (id == notes.id) renamed else error("Unexpected id $id")
        }

        // Then the source is the aligned instance and the meaning is unchanged
        aligned.sourceAttribute.name shouldBe "Notes"
        aligned.asText() shouldContain "from Notes:"
        aligned.instructions shouldBe hasDiabetes.instructions
        aligned.examples shouldBe examples
        aligned.revision shouldBe "rev-1"
        aligned.evaluationProfile shouldBe profile
    }

    @Test
    fun `round trips through serialization as a value expression`() {
        // Given the definition typed as a ValueExpression
        val expression: ValueExpression = hasDiabetes

        // When it is serialized and deserialized
        // Then it is unchanged, including the optional explanation and its absence
        serializeDeserialize(expression) shouldBe hasDiabetes
    }

    @Test
    fun `is identified by its own type discriminator in JSON`() {
        // When the definition is encoded as a ValueExpression
        val json = Json.encodeToString<ValueExpression>(hasDiabetes)

        // Then it carries a discriminator that older expression kinds do not use
        json shouldContain "\"type\":\"AiBoolean\""
        json shouldContain "\"revision\":\"rev-1\""
    }

    @Test
    fun `existing expression kinds are unaffected`() {
        // Given the JSON of an expression stored before AI Booleans existed
        val legacy = """{"type":"Literal","value":"diabetic"}"""

        // When it is decoded
        // Then it reads as before
        Json.decodeFromString<ValueExpression>(legacy) shouldBe Literal("diabetic")
    }

    @Test
    fun `an example without an explanation deserializes`() {
        val json = """{"input":"T1 diab","expected":true}"""
        Json.decodeFromString<AiBooleanExample>(json) shouldBe AiBooleanExample("T1 diab", true)
    }

    @Test
    fun `a Boolean materialises as its canonical case value`() {
        AiBoolean.caseValue(true) shouldBe "true"
        AiBoolean.caseValue(false) shouldBe "false"
    }
}
