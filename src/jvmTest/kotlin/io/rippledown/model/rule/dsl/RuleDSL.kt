package io.rippledown.model.rule.dsl

import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import kotlin.random.Random

fun ruleTree(init: ABSTRACT_RULE_TEMPLATE.() -> Unit) : ROOT_TEMPLATE {
    val n = ROOT_TEMPLATE()
    n.init()
    return n
}

open class ABSTRACT_RULE_TEMPLATE {
    private val conclusionTextToId = mapOf("ROOT" to 999, "A" to 1000, "B" to 1001, "C" to 1002, "ConcA" to 1003, "ConcB" to 1004, "ConcC" to 1005, "ConcD" to 1006)
    protected lateinit var conclusionText: String
    var id = Random.nextInt()
    protected var isStopping: Boolean = false
    protected val conditions = mutableSetOf<Condition>()
    protected val childRules = mutableListOf<RULE_TEMPLATE>()

    open fun child(init: RULE_TEMPLATE.() -> RULE_TEMPLATE) = apply {
        val r = RULE_TEMPLATE()
        r.init()
        childRules.add(r)
    }

    open fun rule(): Rule {
        val result = if (isStopping) Rule(id, null, null, conditions) else Rule(id, null, createConclusion(), conditions)
        childRules.forEach { result.addChild(it.rule()) }
        return result
    }

    fun createConclusion(): Conclusion {
        if (conclusionTextToId.containsKey(conclusionText)) {
            return Conclusion(conclusionTextToId[conclusionText]!!, conclusionText)
        }
        throw IllegalStateException("Unknown conclusion text: $conclusionText")
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
        val result = Rule(Random.nextInt(),null, createConclusion(), conditions)
        childRules.forEach { result.addChild(it.rule()) }
        return result
    }

    init {
        conclusionText = "ROOT"
    }
}

class RULE_TEMPLATE : ABSTRACT_RULE_TEMPLATE() {
    override fun child(init: RULE_TEMPLATE.() -> RULE_TEMPLATE) = apply {
        val r = RULE_TEMPLATE()
        r.init()
        childRules.add(r)
    }

    fun conclusion(init: RULE_TEMPLATE.() -> String) = apply {
        conclusionText = init()
    }

    operator fun String.unaryPlus() {
        conclusionText = this
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
    lateinit var attribute: Attribute
    lateinit var constant: String

    fun condition(): Condition {
        return ContainsText(null, attribute, constant)
    }
}