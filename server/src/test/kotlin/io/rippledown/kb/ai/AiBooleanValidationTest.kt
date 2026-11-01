package io.rippledown.kb.ai

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiBooleanExample
import io.rippledown.model.rule.AiEvaluationProfile
import kotlin.test.Test

internal class AiBooleanValidationTest {
    private val notes = Attribute(1, "clinical_notes")
    private val weight = Attribute(2, "weight")
    private val bmi = Attribute(10, "BMI", AttributeKind.DERIVED)
    private val comment = Attribute(11, "C1", AttributeKind.COMMENT)
    private val attributes = setOf(notes, weight, bmi, comment)
    private val valid = AiBoolean(
        sourceAttribute = notes,
        instructions = "True if the notes affirm a personal history of diabetes.",
        examples = listOf(
            AiBooleanExample("FH diab", false),
            AiBooleanExample("T2 Diab", true)
        ),
        revision = "rev-1",
        evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash")
    )

    private fun messageOf(definition: AiBoolean) =
        shouldThrow<IllegalArgumentException> { validateAiBoolean(definition, attributes) }.message

    @Test
    fun `a complete definition on a primary source is valid`() {
        shouldNotThrowAny { validateAiBoolean(valid, attributes) }
    }

    @Test
    fun `a definition without examples is valid`() {
        shouldNotThrowAny { validateAiBoolean(valid.copy(examples = emptyList()), attributes) }
    }

    @Test
    fun `the source must be an attribute of the knowledge base`() {
        // Given a definition whose source id is unknown here, even though an attribute has that name
        val stranger = Attribute(99, "clinical_notes")

        // Then it is refused: sources resolve by id, not by name
        messageOf(valid.copy(sourceAttribute = stranger)) shouldBe
                "The source attribute \"clinical_notes\" is not in this knowledge base."
    }

    @Test
    fun `a derived attribute cannot be the source`() {
        messageOf(valid.copy(sourceAttribute = bmi)) shouldBe
                "The source of an AI Boolean must be a primary attribute, but \"BMI\" is assigned by the knowledge base."
    }

    @Test
    fun `a comment attribute cannot be the source`() {
        messageOf(valid.copy(sourceAttribute = comment)) shouldBe
                "The source of an AI Boolean must be a primary attribute, but \"C1\" is assigned by the knowledge base."
    }

    @Test
    fun `the criteria cannot be blank`() {
        messageOf(valid.copy(instructions = "  \n")) shouldBe "The interpretation criteria cannot be blank."
    }

    @Test
    fun `the revision cannot be blank`() {
        messageOf(valid.copy(revision = "")) shouldBe "An AI Boolean definition must carry a revision."
    }

    @Test
    fun `an example cannot have blank text`() {
        messageOf(valid.copy(examples = listOf(AiBooleanExample(" ", true)))) shouldBe
                "An example cannot have blank text."
    }

    @Test
    fun `the same example labelled both ways is refused`() {
        // Given two examples with the same text and different labels
        val conflicting = listOf(
            AiBooleanExample("gest diabetes", false),
            AiBooleanExample("gest diabetes", true, "Still diabetes")
        )

        // Then the conflict is reported
        messageOf(valid.copy(examples = conflicting)) shouldBe
                "The example \"gest diabetes\" is labelled both true and false."
    }

    @Test
    fun `an example repeated with the same label is allowed`() {
        val repeated = listOf(
            AiBooleanExample("T2 Diab", true),
            AiBooleanExample("T2 Diab", true, "Type 2")
        )
        shouldNotThrowAny { validateAiBoolean(valid.copy(examples = repeated), attributes) }
    }

    @Test
    fun `examples differing only in whitespace or case are distinct`() {
        // Given examples whose text differs only in case; whether they conflict is for the expert, not the server
        val near = listOf(
            AiBooleanExample("Diabetic", true),
            AiBooleanExample("diabetic", false)
        )
        shouldNotThrowAny { validateAiBoolean(valid.copy(examples = near), attributes) }
    }

    @Test
    fun `an unsupported provider is refused`() {
        val profile = AiEvaluationProfile("openai", "gpt", 0.0, 1, 1)
        messageOf(valid.copy(evaluationProfile = profile)) shouldBe
                "The evaluation profile (provider \"openai\", prompt version 1, schema version 1) is not supported by this server."
    }

    @Test
    fun `an unsupported prompt or schema version is refused`() {
        val prompt = AiEvaluationPolicy.defaultProfile("m").copy(promptVersion = AiEvaluationPolicy.PROMPT_VERSION + 1)
        val schema = AiEvaluationPolicy.defaultProfile("m").copy(schemaVersion = AiEvaluationPolicy.SCHEMA_VERSION + 1)
        shouldThrow<IllegalArgumentException> { validateAiBoolean(valid.copy(evaluationProfile = prompt), attributes) }
        shouldThrow<IllegalArgumentException> { validateAiBoolean(valid.copy(evaluationProfile = schema), attributes) }
    }

    @Test
    fun `the default profile is supported and pins the server's policy versions`() {
        val profile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash")
        profile.provider shouldBe AiEvaluationPolicy.GEMINI
        profile.model shouldBe "gemini-2.5-flash"
        profile.temperature shouldBe 0.0
        profile.promptVersion shouldBe AiEvaluationPolicy.PROMPT_VERSION
        profile.schemaVersion shouldBe AiEvaluationPolicy.SCHEMA_VERSION
        AiEvaluationPolicy.supports(profile) shouldBe true
    }

    @Test
    fun `a new attribute name must be non-blank and unused`() {
        shouldNotThrowAny { validateNewAiAttributeName("hasDiabetes", attributes) }
        shouldThrow<IllegalArgumentException> { validateNewAiAttributeName("  ", attributes) }.message shouldBe
                "An attribute name cannot be blank."
        shouldThrow<IllegalArgumentException> { validateNewAiAttributeName("bmi", attributes) }.message shouldBe
                "An attribute with name \"BMI\" already exists. Choose a different name."
    }
}
