package io.rippledown.model.rule.dsl

import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.NoConclusionRule
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree

fun ruleTree(init: ABSTRACT_RULE_TEMPLATE.() -> Unit) : ROOT_TEMPLATE {
    val n = ROOT_TEMPLATE()
    n.init()
    return n
}

open class ABSTRACT_RULE_TEMPLATE {
    protected lateinit var conclusionDesc: String
    protected var isStopping: Boolean = false
    protected val conditions = mutableSetOf<Condition>()
    protected val childRules = mutableListOf<RULE_TEMPLATE>()

    open fun child(init: RULE_TEMPLATE.() -> RULE_TEMPLATE) = apply {
        val r = RULE_TEMPLATE()
        r.init()
        childRules.add(r)
    }

    open fun rule(): Rule {
        val result = if (isStopping) NoConclusionRule(conditions) else Rule(null, Conclusion(conclusionDesc), conditions)
        childRules.forEach { result.addChild(it.rule()) }
        return result
    }
}

class ROOT_TEMPLATE : ABSTRACT_RULE_TEMPLATE() {
    override fun child(init: RULE_TEMPLATE.() -> RULE_TEMPLATE) = apply {
        val r = RULE_TEMPLATE()
        r.init()
        childRules.add(r)
    }

    fun build(): RuleTree {
        return RuleTree(rule())
    }

    override fun rule(): Rule {
        val result = Rule(null, Conclusion(conclusionDesc), conditions)
        childRules.forEach { result.addChild(it.rule()) }
        return result
    }

    init {
        conclusionDesc = "ROOT"
    }
}

class RULE_TEMPLATE : ABSTRACT_RULE_TEMPLATE() {
    override fun child(init: RULE_TEMPLATE.() -> RULE_TEMPLATE) = apply {
        val r = RULE_TEMPLATE()
        r.init()
        childRules.add(r)
    }

    fun conclusion(init: RULE_TEMPLATE.() -> String) = apply {
        conclusionDesc = init()
    }

    operator fun String.unaryPlus() {
        conclusionDesc = this
    }

    fun stop() = apply {
        isStopping = true
    }

    fun condition(init: CONDITION_TEMPLATE.() -> Unit) = apply {
        val r = CONDITION_TEMPLATE()
        r.init()
        conditions.add(r.condition())
    }
}
class CONDITION_TEMPLATE {
    lateinit var attributeName: String
    lateinit var constant: String

    fun condition(): Condition {
        return ContainsText(Attribute(attributeName), constant)
    }
}