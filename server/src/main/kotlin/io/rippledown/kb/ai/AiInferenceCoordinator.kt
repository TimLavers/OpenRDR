package io.rippledown.kb.ai

import io.rippledown.kb.KBSession
import io.rippledown.log.lazyLogger
import io.rippledown.model.Attribute
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.caseview.IncompleteCategory
import io.rippledown.model.caseview.IncompleteInterpretation
import io.rippledown.model.rule.IncompleteInferenceException
import io.rippledown.model.rule.InferenceOutcome
import io.rippledown.model.rule.evaluation.AiEvaluationResult
import io.rippledown.model.rule.evaluation.DefaultValueExpressionEvaluator
import io.rippledown.model.rule.evaluation.ValueExpressionEvaluator
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

const val DEFAULT_MAX_EVALUATIONS_PER_OPERATION = 100

/**
 * A case interpreted with its AI evaluations prepared. The case carries the
 * derived values and the interpretation, and is a copy, never the stored case.
 */
data class PreparedInterpretation(val case: RDRCase, val provenance: Map<Attribute, AiProvenance>)

/**
 * Lets an operation that interprets cases under the KB lock obtain the AI
 * evaluations it needs without holding the lock during them. The engine reads
 * only the KB's prepared results and stops with a demand when one is missing;
 * the demand propagates out of the locked section as an
 * [IncompleteInferenceException], the classification is obtained here with
 * the lock released, added to the KB's prepared results, and the operation is
 * run again. An operation that needs no evaluation runs once under the lock
 * and cannot be overtaken; one that does is repeated against the live KB, so
 * nothing it computed against a stale KB survives. An operation must
 * therefore interpret before it mutates. A failed classification ends the
 * operation with a typed [InterpretationIncompleteException].
 * See documentation/design/ai_derived_attributes.md.
 */
class AiInferenceCoordinator(
    private val session: KBSession,
    private val evaluationService: AiEvaluationService,
    private val maxEvaluationsPerOperation: Int = DEFAULT_MAX_EVALUATIONS_PER_OPERATION,
    private val evaluator: ValueExpressionEvaluator = DefaultValueExpressionEvaluator(),
    private val operationTimeoutMillis: Long = 120_000
) {
    init {
        require(operationTimeoutMillis > 0) { "The operation timeout must be positive." }
        require(maxEvaluationsPerOperation >= 0) { "The evaluation budget cannot be negative." }
    }
    private val logger = lazyLogger
    private val kb get() = session.kb

    /**
     * Interprets the stored case with the given id, as it stands when the
     * run is published.
     */
    suspend fun interpretStored(caseId: Long): PreparedInterpretation = withPreparedEvaluations(caseId) {
        session.locked {
            val live = kb.getCase(caseId)
                ?: throw InterpretationIncompleteException(abandoned(caseId, "The case no longer exists."))
            interpretUnderLock(live)
        }
    }

    /**
     * Interprets a case the caller holds, such as a cornerstone or a preview,
     * against the current rules and definitions. The case is not modified.
     */
    suspend fun interpret(case: RDRCase): PreparedInterpretation =
        withPreparedEvaluations(case.id) { session.locked { interpretUnderLock(case) } }

    private fun interpretUnderLock(case: RDRCase): PreparedInterpretation {
        val copy = RDRCase(case.caseId, case.data, Interpretation(case.caseId))
        return when (val outcome = kb.ruleTree.infer(copy, kb.definitionResolver, evaluator, kb.evaluationContext())) {
            is InferenceOutcome.Complete -> PreparedInterpretation(outcome.case, outcome.provenance)
            is InferenceOutcome.Stopped -> throw IncompleteInferenceException(outcome)
        }
    }

    /**
     * Runs [operation], obtaining each AI evaluation it demands and running it
     * again, until it completes. The operation takes the KB lock itself, so
     * that an operation which also calls the LLM can keep that outside the
     * lock. [caseId] names the case an incomplete result is reported for.
     */
    suspend fun <T> withPreparedEvaluations(caseId: Long?, operation: () -> T): T =
        withPreparedEvaluations(caseId, false, operation)

    suspend fun <T> withPreparedEvaluations(caseId: Long?, retryAfterFailure: Boolean, operation: () -> T): T =
        prepare(caseId, retryAfterFailure, savedOnly = false, operation)

    suspend fun <T> withSavedEvaluations(caseId: Long?, operation: () -> T): T =
        prepare(caseId, retryAfterFailure = false, savedOnly = true, operation)

    private data class Completed<T>(val value: T)

    private suspend fun <T> prepare(caseId: Long?, retryAfterFailure: Boolean, savedOnly: Boolean, operation: () -> T): T {
        // Box the result so an operation legitimately returning null is not mistaken for a timeout.
        val completed = withContext(Dispatchers.Default) {
            withTimeoutOrNull(operationTimeoutMillis) {
                Completed(prepareWithinDeadline(caseId, retryAfterFailure, savedOnly, operation))
            }
        } ?: throw InterpretationIncompleteException(abandoned(caseId,
            "AI evaluation exceeded the operation time limit. Please retry."))
        return completed.value
    }

    private suspend fun <T> prepareWithinDeadline(caseId: Long?, retryAfterFailure: Boolean, savedOnly: Boolean, operation: () -> T): T {
        var evaluations = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val stopped = try {
                return operation()
            } catch (e: IncompleteInferenceException) {
                e.outcome
            }
            when (stopped) {
                is InferenceOutcome.Incomplete ->
                    throw InterpretationIncompleteException(evaluationFailed(caseId, stopped.attribute, stopped.reason))

                is InferenceOutcome.Demand -> {
                    if (evaluations == maxEvaluationsPerOperation) {
                        throw InterpretationIncompleteException(
                            abandoned(caseId, "The operation needed more than $maxEvaluationsPerOperation AI evaluations.")
                        )
                    }
                    evaluations++
                    logger.info("Evaluating ${stopped.attribute.name} outside the lock (evaluation $evaluations)")
                    val result = if (savedOnly) evaluationService.savedEvaluation(stopped.request)
                    else evaluationService.evaluate(stopped.request, retryAfterFailure)
                    when (result) {
                        is AiEvaluationResult.Success -> kb.preparedEvaluations.add(stopped.request, result)
                        is AiEvaluationResult.Failure ->
                            throw InterpretationIncompleteException(evaluationFailed(caseId, stopped.attribute, result.reason))
                    }
                }
            }
        }
    }

    private fun evaluationFailed(caseId: Long?, attribute: Attribute, reason: String) =
        IncompleteInterpretation(caseId, IncompleteCategory.EVALUATION_FAILED, attribute.name, reason)

    private fun abandoned(caseId: Long?, reason: String) =
        IncompleteInterpretation(caseId, IncompleteCategory.ABANDONED, null, reason)
}
