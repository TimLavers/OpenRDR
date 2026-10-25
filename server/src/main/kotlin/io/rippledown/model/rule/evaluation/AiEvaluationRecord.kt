package io.rippledown.model.rule.evaluation

import io.rippledown.model.rule.AiBoolean

/**
 * A durable, successful classification. The [definition] is the snapshot
 * under which the result was obtained, kept so that an older record can be
 * explained after the live definition is edited. The input itself is not
 * stored, only its hash in the [key]; the evidence is a short excerpt.
 */
data class AiEvaluationRecord(
    val key: AiEvaluationKey,
    val value: Boolean,
    val evidence: String?,
    val definition: AiBoolean,
    val evaluatedAt: Long,
    val requestedModel: String,
    val reportedModel: String?
) {
    fun toResult() = AiEvaluationResult.Success(
        value = value,
        evidence = evidence,
        evaluatedAt = evaluatedAt,
        model = reportedModel ?: requestedModel
    )
}
