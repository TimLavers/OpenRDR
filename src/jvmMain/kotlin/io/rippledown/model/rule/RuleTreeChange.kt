package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.RuleFactory

abstract class RuleTreeChange {
    abstract fun createChanger(tree: RuleTree, ruleFactory: RuleFactory): RuleTreeChanger
}

class ChangeTreeToAddConclusion(private val toBeAdded: Conclusion) : RuleTreeChange() {
    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory): RuleTreeChanger {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "ChangeTreeToAddConclusion(toBeAdded=$toBeAdded)"
    }
}

open class ChangeTreeToRemoveConclusion(internal val toBeRemoved: Conclusion) : RuleTreeChange() {

    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory): RuleTreeChanger {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "ChangeTreeToRemoveConclusion(toBeRemoved=$toBeRemoved)"
    }
}

class ChangeTreeToReplaceConclusion(toBeReplaced: Conclusion, private val replacement: Conclusion) : ChangeTreeToRemoveConclusion(toBeReplaced) {
    override fun createChanger(tree: RuleTree, ruleFactory: RuleFactory): RuleTreeChanger {
        TODO()
    }

    override fun toString(): String {
        return "ChangeTreeToReplaceConclusion(toBeReplaced=$toBeRemoved replacement=$replacement)"
    }
}