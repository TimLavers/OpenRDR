package io.rippledown.model.rule

import io.rippledown.kb.ConclusionProvider
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory

internal fun ConclusionProvider.getAlignedConclusion(provided: Conclusion): Conclusion {
    val conclusionInFactory = getOrCreate(provided.text, provided.variables)
    require(conclusionInFactory.id == provided.id) {
        "Conclusion in factory is $conclusionInFactory, conclusion provided is $provided, which do not match."
    }
    return conclusionInFactory
}

abstract class RuleTreeChange {
    abstract fun alignWith(conclusionFactory: ConclusionProvider): RuleTreeChange
    abstract fun isApplicable(tree: RuleTree, case: RDRCase): Boolean
    abstract fun createChanger(tree: RuleTree, ruleFactory: RuleFactory): RuleTreeChanger

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

class ChangeTreeToAddConclusion(val toBeAdded: Conclusion) : RuleTreeChange() {
    override fun alignWith(conclusionFactory: ConclusionProvider): ChangeTreeToAddConclusion {
        val conclusionInFactory = conclusionFactory.getAlignedConclusion(toBeAdded)
        return ChangeTreeToAddConclusion(conclusionInFactory)
    }

    override fun isApplicable(tree: RuleTree, case: RDRCase) = !tree.apply(case).conclusions().contains(toBeAdded)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) = AddConclusionRuleTreeChanger(tree, ruleFactory, toBeAdded)

    override fun toString() = "ChangeTreeToAddConclusion(toBeAdded=$toBeAdded)"
}

open class ChangeTreeToRemoveConclusion(val toBeRemoved: Conclusion) : RuleTreeChange() {
    override fun alignWith(conclusionFactory: ConclusionProvider): ChangeTreeToRemoveConclusion {
        val conclusionInFactory = conclusionFactory.getAlignedConclusion(toBeRemoved)
        return ChangeTreeToRemoveConclusion(conclusionInFactory)
    }

    override fun isApplicable(tree: RuleTree, case: RDRCase) = tree.apply(case).conclusions().contains(toBeRemoved)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) = RemoveConclusionRuleTreeChanger(tree, ruleFactory, toBeRemoved)

    override fun toString() = "ChangeTreeToRemoveConclusion(toBeRemoved=$toBeRemoved)"
}

class ChangeTreeToReplaceConclusion(val toBeReplaced: Conclusion, val replacement: Conclusion) : RuleTreeChange() {
    override fun alignWith(conclusionFactory: ConclusionProvider): ChangeTreeToReplaceConclusion {
        val toBeReplacedFactoryInstance = conclusionFactory.getAlignedConclusion(toBeReplaced)
        val replacementFactoryInstance = conclusionFactory.getAlignedConclusion(replacement)
        return ChangeTreeToReplaceConclusion(toBeReplacedFactoryInstance, replacementFactoryInstance)
    }

    override fun isApplicable(tree: RuleTree, case: RDRCase) = tree.apply(case).conclusions().contains(toBeReplaced)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) = ReplaceConclusionRuleTreeChanger(tree, ruleFactory, toBeReplaced, replacement)

    override fun toString() = "ChangeTreeToReplaceConclusion(toBeReplaced=$toBeReplaced replacement=$replacement)"
}

class ChangeTreeToAddAssignment(val toBeAdded: AssignValue) : RuleTreeChange() {
    // Assignments do not involve conclusions, so there is nothing to align.
    override fun alignWith(conclusionFactory: ConclusionProvider) = this

    override fun isApplicable(tree: RuleTree, case: RDRCase) = !tree.apply(case).assignments().contains(toBeAdded)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) =
        AddAssignmentRuleTreeChanger(tree, ruleFactory, toBeAdded)

    override fun assignedAttribute() = toBeAdded.attribute

    override fun expressionReferences(resolver: DefinitionResolver) =
        toBeAdded.expression.resolvedFor(toBeAdded.attribute, resolver)?.referencedAttributes() ?: emptySet()

    override fun toString() = "ChangeTreeToAddAssignment(toBeAdded=$toBeAdded)"
}

class ChangeTreeToRemoveAssignment(val toBeRemoved: AssignValue) : RuleTreeChange() {
    override fun alignWith(conclusionFactory: ConclusionProvider) = this

    override fun isApplicable(tree: RuleTree, case: RDRCase) = tree.apply(case).assignments().contains(toBeRemoved)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) =
        RemoveAssignmentRuleTreeChanger(tree, ruleFactory, toBeRemoved)

    override fun assignedAttribute() = toBeRemoved.attribute

    override fun toString() = "ChangeTreeToRemoveAssignment(toBeRemoved=$toBeRemoved)"
}

class ChangeTreeToReplaceAssignment(val toBeReplaced: AssignValue, val replacement: AssignValue) : RuleTreeChange() {
    init {
        require(toBeReplaced.attribute == replacement.attribute) {
            "An assignment can only be replaced by an assignment to the same attribute."
        }
    }

    override fun alignWith(conclusionFactory: ConclusionProvider) = this

    override fun isApplicable(tree: RuleTree, case: RDRCase) = tree.apply(case).assignments().contains(toBeReplaced)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) =
        ReplaceAssignmentRuleTreeChanger(tree, ruleFactory, toBeReplaced, replacement)

    override fun assignedAttribute() = replacement.attribute

    override fun expressionReferences(resolver: DefinitionResolver) =
        replacement.expression.resolvedFor(replacement.attribute, resolver)?.referencedAttributes() ?: emptySet()

    override fun toString() = "ChangeTreeToReplaceAssignment(toBeReplaced=$toBeReplaced replacement=$replacement)"
}