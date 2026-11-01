package io.rippledown.llm

import io.kotest.matchers.shouldBe
import io.rippledown.llm.AiDiabetesEvaluationTest.EvaluationReport
import io.rippledown.llm.AiDiabetesEvaluationTest.Observation
import io.rippledown.model.Attribute
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiBooleanExample
import io.rippledown.model.rule.AiEvaluationProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

/** Exercises report serialization without making any model requests. */
class AiDiabetesEvaluationReportTest {
    private val json = Json { prettyPrint = true }
    private val definition = AiBoolean(
        sourceAttribute = Attribute(1, "clinical_notes"),
        instructions = "Clear personal diabetes history.",
        examples = listOf(AiBooleanExample("T2 Diab", true)),
        revision = "report-test",
        evaluationProfile = AiEvaluationProfile(GEMINI_BOOLEAN_PROVIDER, "test-model", 0.0,
            GEMINI_BOOLEAN_PROMPT_VERSION, GEMINI_BOOLEAN_SCHEMA_VERSION)
    )

    @Test
    fun `report preserves successful negative and failed observations`() {
        // Given the same report types used by the live evaluation, including escaped note text
        val positive = Observation("Recorded: \"T2 Diab\"\nFollow-up", true, true, true,
            "T2 Diab", "test-model-version", 1, 12L, null)
        val negative = Observation("FH diab", false, false, false, null, "test-model-version", 2, 15L, null)
        val failed = Observation("Diabetic", false, true, null, null, null, 3, 30L, "TIMEOUT")
        val report = EvaluationReport(definition, listOf(positive, negative, failed))

        // When the report is encoded and read back
        val encoded = json.encodeToString(report)
        val decoded = json.decodeFromString<EvaluationReport>(encoded)

        // Then definition, labels, evidence, model version, repetitions and failures survive
        decoded shouldBe report
        val observations = json.parseToJsonElement(encoded).jsonObject.getValue("observations").jsonArray
        observations[0].jsonObject.getValue("actual").jsonPrimitive.boolean shouldBe true
        observations[1].jsonObject.getValue("actual").jsonPrimitive.boolean shouldBe false
        observations[2].jsonObject.getValue("actual") shouldBe JsonNull
    }

    @Test
    fun `report can be serialized before collecting observations`() {
        // Given the definition and an empty result list
        val report = EvaluationReport(definition, emptyList())

        // When serialized without running Gemini
        val encoded = json.encodeToString(report)

        // Then even an empty report has the serializer and retains the definition
        json.decodeFromString<EvaluationReport>(encoded) shouldBe report
        json.parseToJsonElement(encoded).jsonObject.getValue("observations").jsonArray.size shouldBe 0
    }
}
