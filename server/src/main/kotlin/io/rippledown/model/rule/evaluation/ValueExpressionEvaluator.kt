package io.rippledown.model.rule.evaluation

import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.ValueExpression

/**
 * Identifies one classification: the exact definition and the exact source
 * text. Two requests are the same evaluation if and only if these are equal,
 * which is why a definition edit (new revision) or a renamed source (same id)
 * behave as the design requires. Persistence derives its durable key from
 * this. See documentation/tickets/ai_derived_attributes_design.md.
 */
data class AiEvaluationRequest(val definition: AiBoolean, val input: String)

/**
 * What a classification produced, once the coordinator has obtained it:
 * either a durable classification or the reason none could be had. A
 * failure is memoised for the operation only; it is never a stored value.
 */
sealed class AiEvaluationResult {
    data class Success(
        val value: Boolean,
        val evidence: String?,
        val evaluatedAt: Long,
        val model: String
    ) : AiEvaluationResult()

    data class Failure(val reason: String) : AiEvaluationResult()
}

/**
 * The prepared results an inference pass may read. Immutable, so that every
 * pass of one speculative run sees the same results; the coordinator builds
 * an extended context and restarts the run when a demand is satisfied.
 */
class EvaluationContext private constructor(
    private val results: Map<AiEvaluationRequest, AiEvaluationResult>
) {
    fun resultFor(request: AiEvaluationRequest): AiEvaluationResult? = results[request]

    fun with(request: AiEvaluationRequest, result: AiEvaluationResult) =
        EvaluationContext(results + (request to result))

    companion object {
        val EMPTY = EvaluationContext(emptyMap())
    }
}

/**
 * The outcome of evaluating a resolved expression for a case. Only [Value]
 * assigns anything. [NoInput] is an ordinary successful non-assignment,
 * so absence conditions keep their meaning. [NeedsEvaluation] and [Failed]
 * end the speculative attempt: neither may be read as an absent attribute.
 */
sealed class EvaluationOutcome {
    data class Value(val value: String, val provenance: AiProvenance? = null) : EvaluationOutcome()
    data object NoInput : EvaluationOutcome()
    data class NeedsEvaluation(val request: AiEvaluationRequest) : EvaluationOutcome()
    data class Failed(val reason: String) : EvaluationOutcome()
}

/**
 * Evaluates a resolved value expression for a case. The rule engine calls
 * this after [ByDefinition] resolution; an implementation performs no
 * network or persistence I/O.
 */
interface ValueExpressionEvaluator {
    fun evaluate(expression: ValueExpression, case: RDRCase, context: EvaluationContext): EvaluationOutcome
}

/**
 * Evaluates local expressions directly and AI Booleans from the prepared
 * results in the context.
 */
class DefaultValueExpressionEvaluator : ValueExpressionEvaluator {
    override fun evaluate(expression: ValueExpression, case: RDRCase, context: EvaluationContext) =
        when (expression) {
            is AiBoolean -> evaluateAi(expression, case, context)
            is ByDefinition -> error("A ByDefinition expression must be resolved to a concrete expression before evaluation.")
            else -> expression.evaluate(case)?.let { EvaluationOutcome.Value(it) } ?: EvaluationOutcome.NoInput
        }

    private fun evaluateAi(definition: AiBoolean, case: RDRCase, context: EvaluationContext): EvaluationOutcome {
        val input = definition.inputFrom(case) ?: return EvaluationOutcome.NoInput
        val request = AiEvaluationRequest(definition, input)
        return when (val result = context.resultFor(request)) {
            null -> EvaluationOutcome.NeedsEvaluation(request)
            is AiEvaluationResult.Failure -> EvaluationOutcome.Failed(result.reason)
            is AiEvaluationResult.Success -> EvaluationOutcome.Value(
                AiBoolean.caseValue(result.value),
                AiProvenance(
                    sourceName = definition.sourceAttribute.name,
                    instructions = definition.instructions,
                    evidence = result.evidence,
                    evaluatedAt = result.evaluatedAt,
                    model = result.model,
                    revision = definition.revision
                )
            )
        }
    }
}

/**
 * The text to classify: the source's value in the latest episode, exactly
 * as held, or null if the case has no episodes or the latest value is
 * absent or blank. Blankness is checked without altering the text.
 */
fun AiBoolean.inputFrom(case: RDRCase): String? {
    if (case.numberOfEpisodes() == 0) return null
    return case.latestValue(sourceAttribute)?.takeIf { it.isNotBlank() }
}
