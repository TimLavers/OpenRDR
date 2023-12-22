package io.rippledown.model.rule

import io.rippledown.kb.ConclusionProvider
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory

internal fun ConclusionProvider.getAlignedConclusion(provided: Conclusion): Conclusion {
    val conclusionInFactory = getOrCreate(provided.text)
    require(conclusionInFactory.id == provided.id) {
        "Conclusion in factory is $conclusionInFactory, conclusion provided is $provided, which do not match."
    }
    return conclusionInFactory
}

abstract class RuleTreeChange {
    abstract fun alignWith(conclusionFactory: ConclusionProvider): RuleTreeChange
    abstract fun isApplicable(tree: RuleTree, case: RDRCase): Boolean
    abstract fun createChanger(tree: RuleTree, ruleFactory: RuleFactory): RuleTreeChanger
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