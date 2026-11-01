package io.rippledown.kb.ai

import io.rippledown.model.rule.evaluation.AiEvaluationRequest

/**
 * What the provider adapter returns for one request: a validated
 * classification, or the reason none was obtained. The adapter owns its
 * attempt and deadline budget; it never returns a coerced Boolean.
 */
sealed class AiClassification {
    data class Classified(
        val value: Boolean,
        val evidence: String?,
        val reportedModel: String?
    ) : AiClassification()

    data class Failed(val reason: String) : AiClassification()
}

interface AiBooleanClassifier {
    suspend fun classify(request: AiEvaluationRequest): AiClassification
}
