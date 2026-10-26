package io.rippledown.kb.ai

import io.rippledown.llm.BooleanClassification
import io.rippledown.llm.GeminiBooleanClassifier
import io.rippledown.llm.GeminiStructuredTransport
import io.rippledown.model.rule.evaluation.AiEvaluationRequest

/**
 * The production [AiBooleanClassifier]: delegates to the Gemini adapter in
 * the llm module, which owns the prompt contract, validation and attempt
 * budget.
 */
class GeminiAiBooleanClassifier(
    private val classifier: GeminiBooleanClassifier = GeminiBooleanClassifier(GeminiStructuredTransport())
) : AiBooleanClassifier {
    override suspend fun classify(request: AiEvaluationRequest) =
        classifier.classify(request.definition, request.input).toAiClassification()
}

fun BooleanClassification.toAiClassification() = when (this) {
    is BooleanClassification.Classified -> AiClassification.Classified(value, evidence, reportedModel)
    is BooleanClassification.Failed -> AiClassification.Failed("$kind: $message")
}
