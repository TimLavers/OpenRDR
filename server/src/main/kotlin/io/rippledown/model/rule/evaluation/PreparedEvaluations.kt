package io.rippledown.model.rule.evaluation

/**
 * The classifications a knowledge base has obtained, held so that inference
 * under the KB lock can read them without any I/O. Results are added outside
 * the lock, one per classification, and read by every interpretation, so the
 * map is replaced on write and shared on read: a context taken for a run is
 * immutable for the life of that run. Only successes are kept; a failure is
 * reported to the operation that met it and may be retried later.
 */
class PreparedEvaluations {
    @Volatile
    private var results: Map<AiEvaluationRequest, AiEvaluationResult.Success> = emptyMap()

    @Synchronized
    fun add(request: AiEvaluationRequest, result: AiEvaluationResult.Success) {
        results = results + (request to result)
    }

    fun context(): EvaluationContext = EvaluationContext.of(results)
}
