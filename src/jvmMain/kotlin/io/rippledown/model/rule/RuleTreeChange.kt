package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory

abstract class RuleTreeChange {
    abstract fun isApplicable(tree: RuleTree, case: RDRCase): Boolean
    abstract fun createChanger(tree: RuleTree, ruleFactory: RuleFactory): RuleTreeChanger
}

class ChangeTreeToAddConclusion(private val toBeAdded: Conclusion) : RuleTreeChange() {
    override fun isApplicable(tree: RuleTree, case: RDRCase) = !tree.apply(case).conclusions().contains(toBeAdded)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) = AddConclusionRuleTreeChanger(tree, ruleFactory, toBeAdded)

    override fun toString() = "ChangeTreeToAddConclusion(toBeAdded=$toBeAdded)"
}

open class ChangeTreeToRemoveConclusion(private val toBeRemoved: Conclusion) : RuleTreeChange() {
    override fun isApplicable(tree: RuleTree, case: RDRCase) = tree.apply(case).conclusions().contains(toBeRemoved)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) = RemoveConclusionRuleTreeChanger(tree, ruleFactory, toBeRemoved)

    override fun toString() = "ChangeTreeToRemoveConclusion(toBeRemoved=$toBeRemoved)"
}

class ChangeTreeToReplaceConclusion(private val toBeReplaced: Conclusion, private val replacement: Conclusion) : RuleTreeChange() {
    override fun isApplicable(tree: RuleTree, case: RDRCase) = tree.apply(case).conclusions().contains(toBeReplaced)

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory) = ReplaceConclusionRuleTreeChanger(tree, ruleFactory, toBeReplaced, replacement)

    override fun toString() = "ChangeTreeToReplaceConclusion(toBeReplaced=$toBeReplaced replacement=$replacement)"
}