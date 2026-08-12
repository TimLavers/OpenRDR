package io.rippledown.model.rule.dsl

import io.rippledown.model.Attribute
import io.rippledown.model.CommentFactory
import io.rippledown.model.ConditionFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.containsText
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import kotlin.random.Random

fun ruleTree(commentFactory: CommentFactory, init: AbstractRuleTemplate.() -> Unit): RootTemplate {
    val n = RootTemplate(commentFactory)
    n.init()
    return n
}

open class AbstractRuleTemplate(val commentFactory: CommentFactory) {

    protected lateinit var commentText: String
    var id = Random.nextInt()
    protected var isStopping: Boolean = false
    protected val conditions = mutableSetOf<Condition>()
    protected val childRules = mutableListOf<RuleTemplate>()

    open fun child(init: RuleTemplate.() -> RuleTemplate) = apply {
        val r = RuleTemplate(commentFactory)
        r.init()
        childRules.add(r)
    }

    open fun rule(): Rule {
        val assignment = if (isStopping) null else createComment()
        val result = Rule(id, null, conditions, mutableSetOf(), assignment)
        childRules.forEach { result.addChild(it.rule()) }
        return result
    }

    fun createComment() = commentFactory.comment(commentText)
}

class RootTemplate(commentFactory: CommentFactory) : AbstractRuleTemplate(commentFactory) {
    override fun child(init: RuleTemplate.() -> RuleTemplate) = apply {
        val r = RuleTemplate(commentFactory)
        r.init()
        childRules.add(r)
    }

    fun build(): RuleTree {
        return RuleTree(rule())
    }

    override fun rule(): Rule {
        val result = Rule(Random.nextInt(), null, conditions, mutableSetOf(), createComment())
        childRules.forEach { result.addChild(it.rule()) }
        return result
    }

    init {
        commentText = "ROOT"
    }
}

class RuleTemplate(commentFactory: CommentFactory) : AbstractRuleTemplate(commentFactory) {
    override fun child(init: RuleTemplate.() -> RuleTemplate) = apply {
        val r = RuleTemplate(commentFactory)
        r.init()
        childRules.add(r)
    }

    fun comment(init: RuleTemplate.() -> String) = apply {
        commentText = init()
    }

    operator fun String.unaryPlus() {
        commentText = this
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
