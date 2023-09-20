package io.rippledown.model.rule.dsl

import io.rippledown.kb.ConclusionProvider
import io.rippledown.model.Attribute
import io.rippledown.model.ConditionFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.containsText
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import kotlin.random.Random

fun ruleTree(conclusionFactory: ConclusionProvider, init: AbstractRuleTemplate.() -> Unit): RootTemplate {
    val n = RootTemplate(conclusionFactory)
    n.init()
    return n
}

open class AbstractRuleTemplate(val conclusionFactory: ConclusionProvider) {

    protected lateinit var conclusionText: String
    var id = Random.nextInt()
    protected var isStopping: Boolean = false
    protected val conditions = mutableSetOf<Condition>()
    protected val childRules = mutableListOf<RuleTemplate>()

    open fun child(init: RuleTemplate.() -> RuleTemplate) = apply {
        val r = RuleTemplate(conclusionFactory)
        r.init()
        childRules.add(r)
    }

    open fun rule(): Rule {
        val result = if (isStopping) Rule(id, null, null, conditions) else Rule(id, null, createConclusion(), conditions)
        childRules.forEach { result.addChild(it.rule()) }
        return result
    }

    fun createConclusion() = conclusionFactory.getOrCreate(conclusionText)
}

class RootTemplate(conclusionFactory: ConclusionProvider) : AbstractRuleTemplate(conclusionFactory) {
    override fun child(init: RuleTemplate.() -> RuleTemplate) = apply {
        val r = RuleTemplate(conclusionFactory)
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

class RuleTemplate(conclusionFactory: ConclusionProvider) : AbstractRuleTemplate(conclusionFactory) {
    override fun child(init: RuleTemplate.() -> RuleTemplate) = apply {
        val r = RuleTemplate(conclusionFactory)
        r.init()
        childRules.add(r)
    }

    fun conclusion(init: RuleTemplate.() -> String) = apply {
        conclusionText = init()
    }

    operator fun String.unaryPlus() {
        conclusionText = this
    }

    fun stop() = apply {
        isStopping = true
    }

    fun condition(conditionFactory: ConditionFactory, init: CONDITION_TEMPLATE.() -> Unit) = apply {
        val r = CONDITION_TEMPLATE(conditionFactory)
        r.init()
        conditions.add(r.condition())
    }
}
class CONDITION_TEMPLATE(private val conditionFactory: ConditionFactory) {
    lateinit var attribute: Attribute
    lateinit var constant: String

    fun condition(): Condition {
        return conditionFactory.getOrCreate(containsText(null, attribute, constant))
    }
}