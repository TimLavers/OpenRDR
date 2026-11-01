package io.rippledown.kb.ai

import io.kotest.matchers.shouldBe
import io.rippledown.llm.BooleanClassification
import io.rippledown.llm.ClassificationFailure
import io.rippledown.llm.GeminiBooleanClassifier
import io.rippledown.llm.StructuredGenerationRequest
import io.rippledown.llm.StructuredGenerationResponse
import io.rippledown.llm.StructuredGenerationTransport
import io.rippledown.model.Attribute
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class GeminiAiBooleanClassifierTest {
    private val hasDiabetes = AiBoolean(
        sourceAttribute = Attribute(1, "clinical_notes"),
        instructions = "True if the notes affirm a personal history of diabetes.",
        revision = "rev-1",
        evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash")
    )
    private val request = AiEvaluationRequest(hasDiabetes, "T2 Diab")

    private fun transport(text: String?, finishReason: String? = "STOP") = object : StructuredGenerationTransport {
        override suspend fun generate(request: StructuredGenerationRequest) =
            StructuredGenerationResponse(text, finishReason, null, "gemini-2.5-flash-001")
    }

    @Test
    fun `a classification is passed through with its evidence and model`() = runTest {
        // Given a Gemini classifier whose transport answers true
        val classifier = GeminiAiBooleanClassifier { GeminiBooleanClassifier(transport("""{"value":true,"evidence":"T2 Diab"}""")) }

        // When the request is classified
        val result = classifier.classify(request)

        // Then the server-side classification carries the same data
        result shouldBe AiClassification.Classified(true, "T2 Diab", "gemini-2.5-flash-001")
    }

    @Test
    fun `a failure becomes a reason naming the kind`() = runTest {
        val classifier = GeminiAiBooleanClassifier { GeminiBooleanClassifier(transport("", "SAFETY")) }

        val result = classifier.classify(request)

        result shouldBe AiClassification.Failed("${ClassificationFailure.REFUSED}: The response ended with SAFETY.")
    }

    @Test
    fun `the default profile is one the classifier supports`() = runTest {
        val classifier = GeminiAiBooleanClassifier { GeminiBooleanClassifier(transport("""{"value":false,"evidence":null}""")) }

        classifier.classify(request) shouldBe AiClassification.Classified(false, null, "gemini-2.5-flash-001")
    }

    @Test
    fun `the reason is shaped from the llm failure`() {
        val failed = BooleanClassification.Failed(ClassificationFailure.RATE_LIMITED, "Quota exceeded")
        failed.toAiClassification() shouldBe AiClassification.Failed("RATE_LIMITED: Quota exceeded")
    }
}
