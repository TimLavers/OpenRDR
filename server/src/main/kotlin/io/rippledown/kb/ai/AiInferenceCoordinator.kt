package io.rippledown.kb.ai

import io.rippledown.kb.KBSession
import io.rippledown.log.lazyLogger
import io.rippledown.model.Attribute
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.DefinitionResolver
import io.rippledown.model.rule.InferenceOutcome
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.ValueExpression
import io.rippledown.model.rule.evaluation.DefaultValueExpressionEvaluator
import io.rippledown.model.rule.evaluation.EvaluationContext
import io.rippledown.model.rule.evaluation.ValueExpressionEvaluator

const val DEFAULT_MAX_RESTARTS = 3

/**
 * The result of interpreting a case with its AI evaluations prepared. Only
 * [Ready] is an interpretation: its case carries the derived values and the
 * interpretation, and is a copy, never the stored case.
 */
sealed class PreparedInterpretation {
    data class Ready(val case: RDRCase, val provenance: Map<Attribute, AiProvenance>) : PreparedInterpretation()
    data class Incomplete(val attribute: Attribute, val reason: String) : PreparedInterpretation()
    data class Abandoned(val reason: String) : PreparedInterpretation()
}

/**
 * Runs inference with the KB lock held only to take a snapshot and to check
 * it is still current. The classification, with its network and database
 * I/O, happens between those two short locked phases, so other users of the
 * KB are never blocked on the provider. If the rules, definitions or case
 * changed meanwhile, the prepared interpretation is discarded and the run
 * restarts from a fresh snapshot, keeping the classifications already
 * obtained: a result is identified by its definition and input, so it stays
 * valid whatever else changed. See documentation/tickets/ai_derived_attributes_design.md.
 */
class AiInferenceCoordinator(
    private val session: KBSession,
    private val evaluationService: AiEvaluationService,
    private val maxRestarts: Int = DEFAULT_MAX_RESTARTS,
    private val evaluator: ValueExpressionEvaluator = DefaultValueExpressionEvaluator()
) {
    private val logger = lazyLogger
    private val kb get() = session.kb

    /**
     * Interprets the stored case with the given id, as it stands when the
     * run is published.
     */
    suspend fun interpretStored(caseId: Long): PreparedInterpretation = run { kb.getCase(caseId) }

    /**
     * Interprets a case the caller holds, such as a cornerstone or a preview,
     * against the current rules and definitions. The case is not modified.
     */
    suspend fun interpret(case: RDRCase): PreparedInterpretation = run { case }

    private suspend fun run(caseSource: () -> RDRCase?): PreparedInterpretation {
        var context = EvaluationContext.EMPTY
        repeat(maxRestarts + 1) { attempt ->
            val snapshot = session.locked { caseSource()?.let { Snapshot.of(it, kb.ruleTree, kb.derivedDefinitionManager.all()) } }
                ?: return PreparedInterpretation.Abandoned("The case no longer exists.")
            val (prepared, extended) = inferWithEvaluations(snapshot, context)
            context = extended
            val current = session.locked {
                val live = caseSource()
                live != null && snapshot.isCurrent(live, kb.ruleTree, kb.derivedDefinitionManager.all())
            }
            if (current) return prepared
            logger.info("Interpretation of ${snapshot.case.name} was overtaken by a change (attempt ${attempt + 1}); restarting")
        }
        return PreparedInterpretation.Abandoned("The knowledge base kept changing while the case was being interpreted.")
    }

    private suspend fun inferWithEvaluations(
        snapshot: Snapshot,
        initial: EvaluationContext
    ): Pair<PreparedInterpretation, EvaluationContext> {
        var context = initial
        while (true) {
            when (val outcome = snapshot.tree.infer(snapshot.case, snapshot.resolver, evaluator, context)) {
                is InferenceOutcome.Demand ->
                    context = context.with(outcome.request, evaluationService.evaluate(outcome.request))
                is InferenceOutcome.Complete ->
                    return PreparedInterpretation.Ready(outcome.case, outcome.provenance) to context
                is InferenceOutcome.Incomplete ->
                    return PreparedInterpretation.Incomplete(outcome.attribute, outcome.reason) to context
            }
        }
    }

    /**
     * What a run reads, copied so that the run shares nothing with the live
     * KB, and what it compares against to see whether the live KB has moved.
     */
    private class Snapshot private constructor(
        val case: RDRCase,
        val tree: RuleTree,
        private val definitions: Map<Int, ValueExpression>,
        private val shape: Set<RuleShape>
    ) {
        val resolver: DefinitionResolver = { definitions[it.id] }

        fun isCurrent(liveCase: RDRCase, liveTree: RuleTree, liveDefinitions: Map<Int, ValueExpression>) =
            liveCase.hasSameDataAs(case) && liveTree.shape() == shape && liveDefinitions == definitions

        companion object {
            fun of(case: RDRCase, tree: RuleTree, definitions: Map<Int, ValueExpression>) = Snapshot(
                case = RDRCase(case.caseId, case.data, Interpretation(case.caseId)),
                tree = tree.copy(),
                definitions = definitions,
                shape = tree.shape()
            )
        }
    }
}

private data class RuleShape(val id: Int, val parentId: Int?, val conditions: Set<Condition>, val assignment: AssignValue?)

private fun RuleTree.shape() = rules().map { RuleShape(it.id, it.parent?.id, it.conditions, it.assignment) }.toSet()
