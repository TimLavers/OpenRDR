package io.rippledown.kb.ai

import io.rippledown.model.rule.evaluation.AiEvaluationKey
import io.rippledown.model.rule.evaluation.AiEvaluationRecord
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.model.rule.evaluation.AiEvaluationResult
import io.rippledown.model.rule.evaluation.key
import io.rippledown.persistence.AiEvaluationStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

const val DEFAULT_FAILURE_COOLDOWN_MILLIS = 10_000L

/**
 * Obtains the classification for a request: from the KB's evaluation store
 * if one is saved, otherwise from the classifier, persisting the result
 * before anyone uses it. Identical concurrent requests share one in-flight
 * classification, which runs in the service's own [scope] so that a waiter
 * cancelling does not cancel the result another waiter needs. Failures are
 * remembered for [failureCooldownMillis] to avoid request storms; they are
 * never persisted. Nothing here is called under the KB lock.
 * See documentation/design/ai_derived_attributes.md.
 */
class AiEvaluationService(
    private val store: AiEvaluationStore,
    private val classifier: AiBooleanClassifier,
    private val clock: () -> Long = System::currentTimeMillis,
    private val failureCooldownMillis: Long = DEFAULT_FAILURE_COOLDOWN_MILLIS,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val inFlight = ConcurrentHashMap<AiEvaluationRequest, Deferred<AiEvaluationResult>>()
    private val recentFailures = ConcurrentHashMap<AiEvaluationKey, RecentFailure>()

    /** Reads provenance without giving an explanation request permission to classify new input. */
    suspend fun savedEvaluation(request: AiEvaluationRequest): AiEvaluationResult = withContext(Dispatchers.IO) {
        try {
            store.recordFor(request.key())?.toResult()
                ?: AiEvaluationResult.Failure("No saved evaluation exists for this definition and input.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AiEvaluationResult.Failure("Saved evaluations could not be read: ${e.message}")
        }
    }

    suspend fun evaluate(request: AiEvaluationRequest, retryAfterFailure: Boolean = false): AiEvaluationResult {
        val key = request.key()
        if (retryAfterFailure) {
            recentFailures.remove(key)
        } else {
            memoisedFailure(key)?.let { return it }
        }
        val deferred = inFlight.computeIfAbsent(request) {
            scope.async(start = CoroutineStart.LAZY) { obtain(request, key) }.also { started ->
                started.invokeOnCompletion { inFlight.remove(request, started) }
            }
        }
        deferred.start()
        return deferred.await()
    }

    private fun memoisedFailure(key: AiEvaluationKey): AiEvaluationResult.Failure? {
        val recent = recentFailures[key] ?: return null
        if (clock() - recent.at < failureCooldownMillis) return recent.failure
        recentFailures.remove(key, recent)
        return null
    }

    private suspend fun obtain(request: AiEvaluationRequest, key: AiEvaluationKey): AiEvaluationResult {
        val saved = try {
            store.recordFor(key)
        } catch (e: Exception) {
            return failure(key, "Saved evaluations could not be read: ${e.message}")
        }
        if (saved != null) return saved.toResult()
        return when (val classification = classify(request)) {
            is AiClassification.Failed -> failure(key, classification.reason)
            is AiClassification.Classified -> persist(request, key, classification)
        }
    }

    private suspend fun classify(request: AiEvaluationRequest): AiClassification =
        try {
            classifier.classify(request)
        } catch (e: TimeoutCancellationException) {
            AiClassification.Failed("The evaluation timed out.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AiClassification.Failed(e.message ?: e.toString())
        }

    private fun persist(
        request: AiEvaluationRequest,
        key: AiEvaluationKey,
        classification: AiClassification.Classified
    ): AiEvaluationResult {
        val record = AiEvaluationRecord(
            key = key,
            value = classification.value,
            evidence = classification.evidence,
            definition = request.definition,
            evaluatedAt = clock(),
            requestedModel = request.definition.evaluationProfile.model,
            reportedModel = classification.reportedModel
        )
        return try {
            store.insertIfAbsent(record).toResult()
        } catch (e: Exception) {
            failure(key, "The evaluation could not be saved: ${e.message}")
        }
    }

    private fun failure(key: AiEvaluationKey, reason: String): AiEvaluationResult.Failure {
        val failure = AiEvaluationResult.Failure(reason)
        recentFailures[key] = RecentFailure(failure, clock())
        return failure
    }

    private data class RecentFailure(val failure: AiEvaluationResult.Failure, val at: Long)
}
