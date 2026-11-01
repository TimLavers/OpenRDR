package io.rippledown.llm

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.model.Attribute
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiBooleanExample
import io.rippledown.model.rule.AiEvaluationProfile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

/**
 * Calls Gemini. Like [GeminiTest], this needs the API key and is run on
 * demand, not as part of the offline build.
 */
class GeminiStructuredTransportTest {
    private val hasDiabetes = AiBoolean(
        sourceAttribute = Attribute(1, "clinical_notes"),
        instructions = "True if the notes affirm that the patient personally has diabetes of any type. " +
                "A family history alone is false. Medication or test results alone are not enough.",
        examples = listOf(
            AiBooleanExample("FH diab", false, "Family history only"),
            AiBooleanExample("T2 Diab", true)
        ),
        revision = "live",
        evaluationProfile = AiEvaluationProfile(
            provider = GEMINI_BOOLEAN_PROVIDER,
            model = GEMINI_MODEL,
            temperature = 0.0,
            promptVersion = GEMINI_BOOLEAN_PROMPT_VERSION,
            schemaVersion = GEMINI_BOOLEAN_SCHEMA_VERSION
        )
    )
    private val classifier = GeminiBooleanClassifier(GeminiStructuredTransport())

    @Test
    fun `an affirmative note classifies true with verbatim evidence`(): Unit = runBlocking {
        val result = classifier.classify(hasDiabetes, "54M. T2 Diab since 2019, on metformin.")

        val classified = result.shouldBeInstanceOf<BooleanClassification.Classified>()
        classified.value shouldBe true
        classified.evidence?.let { "54M. T2 Diab since 2019, on metformin.".contains(it) shouldBe true }
    }

    @Test
    fun `a family history note classifies false`(): Unit = runBlocking {
        val result = classifier.classify(hasDiabetes, "Mother had diabetes. Pt well, no complaints.")

        result.shouldBeInstanceOf<BooleanClassification.Classified>().value shouldBe false
    }
}
