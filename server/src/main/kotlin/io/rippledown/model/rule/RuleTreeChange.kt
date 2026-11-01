package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory
import io.rippledown.model.rule.evaluation.EvaluationContext

abstract class RuleTreeChange {
    abstract fun isApplicable(
        tree: RuleTree, case: RDRCase,
        resolver: DefinitionResolver = NO_DEFINITIONS, context: EvaluationContext = EvaluationContext.EMPTY
    ): Boolean
    abstract fun createChanger(
        tree: RuleTree, ruleFactory: RuleFactory,
        resolver: DefinitionResolver = NO_DEFINITIONS, context: EvaluationContext = EvaluationContext.EMPTY
    ): RuleTreeChanger

    /**
     * The derived attribute whose assignment this change adds, removes or
     * replaces, or null for changes that do not involve an assignment.
     */
    open fun assignedAttribute(): Attribute? = null

    /**
     * The attributes referenced by the value expression that this change
     * introduces, if any. A [ByDefinition] expression's references are those
     * of the stored definition given by [resolver].
     */
    open fun expressionReferences(resolver: DefinitionResolver = NO_DEFINITIONS): Set<Attribute> = emptySet()
}

class ChangeTreeToAddAssignment(val toBeAdded: AssignValue) : RuleTreeChange() {
    override fun isApplicable(tree: RuleTree, case: RDRCase, resolver: DefinitionResolver, context: EvaluationContext) =
        !tree.apply(case, resolver, context).assignments().contains(toBeAdded)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory, resolver: DefinitionResolver, context: EvaluationContext) =
        AddAssignmentRuleTreeChanger(tree, ruleFactory, toBeAdded, resolver, context)

    override fun assignedAttribute() = toBeAdded.attribute

    override fun expressionReferences(resolver: DefinitionResolver) =
        toBeAdded.expression.resolvedFor(toBeAdded.attribute, resolver)?.referencedAttributes() ?: emptySet()

    override fun toString() = "ChangeTreeToAddAssignment(toBeAdded=$toBeAdded)"
}

class ChangeTreeToRemoveAssignment(val toBeRemoved: AssignValue) : RuleTreeChange() {
    override fun isApplicable(tree: RuleTree, case: RDRCase, resolver: DefinitionResolver, context: EvaluationContext) =
        tree.apply(case, resolver, context).assignments().contains(toBeRemoved)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory, resolver: DefinitionResolver, context: EvaluationContext) =
        RemoveAssignmentRuleTreeChanger(tree, ruleFactory, toBeRemoved, resolver, context)

    override fun assignedAttribute() = toBeRemoved.attribute

    override fun toString() = "ChangeTreeToRemoveAssignment(toBeRemoved=$toBeRemoved)"
}

class ChangeTreeToReplaceAssignment(val toBeReplaced: AssignValue, val replacement: AssignValue) : RuleTreeChange() {
    init {
        // A derived value is replaced on its own attribute. Comments are the
        // exception: each comment text has its own attribute, so replacing a
        // comment assigns a different attribute, with leaf-most suppression
        // retracting the original.
        require(
            toBeReplaced.attribute == replacement.attribute ||
                    (toBeReplaced.attribute.kind == AttributeKind.COMMENT &&
                            replacement.attribute.kind == AttributeKind.COMMENT)
        ) {
            "An assignment can only be replaced by an assignment to the same attribute."
        }
    }

    override fun isApplicable(tree: RuleTree, case: RDRCase, resolver: DefinitionResolver, context: EvaluationContext) =
        tree.apply(case, resolver, context).assignments().contains(toBeReplaced)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory, resolver: DefinitionResolver, context: EvaluationContext) =
        ReplaceAssignmentRuleTreeChanger(tree, ruleFactory, toBeReplaced, replacement, resolver, context)

    override fun assignedAttribute() = replacement.attribute

    override fun expressionReferences(resolver: DefinitionResolver) =
        replacement.expression.resolvedFor(replacement.attribute, resolver)?.referencedAttributes() ?: emptySet()

    override fun toString() = "ChangeTreeToReplaceAssignment(toBeReplaced=$toBeReplaced replacement=$replacement)"
}
