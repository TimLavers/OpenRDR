package io.rippledown.kb.scripts

import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.model.condition.*

const val addedConditionBeforeSessionStarted = "Session not started yet. Please define the case and action before adding a condition"
val textAttribute = Attribute("Text")
val numberAttribute = Attribute("Value")

fun build(f: BuildTemplate.() -> Unit): BuildTemplate {
    val template = BuildTemplate()
    template.f()
    return template
}

class BuildTemplate {
    val defaultDate = 1659752689505
    private val kb = KB("")

    fun case(name: String, data: String) {
        val caseBuilder = RDRCaseBuilder()
        caseBuilder.addResult(textAttribute, defaultDate, TestResult(data))
        val case = caseBuilder.build(name)
        kb.addCase(case)
    }

    fun case(i: Int) {
        val caseBuilder = RDRCaseBuilder()
        caseBuilder.addResult(numberAttribute, defaultDate, TestResult("$i"))
        val case = caseBuilder.build("$i")
        kb.addCase(case)
    }

    fun session(s: SessionTemplate.() -> Unit): SessionTemplate {
        val template = SessionTemplate(kb)
        template.s()
        return template
    }

    fun requireInterpretation(caseName: String, vararg expectedConclusions: String) {
        val case = kb.getCaseByName(caseName)
        kb.interpret(case)
        case.interpretation.conclusions() shouldBe expectedConclusions.map { Conclusion(it) }.toSet()
    }
}

class SessionTemplate( val kb: KB) {
    lateinit var case: RDRCase
//    lateinit var action: RuleTreeChange
//    lateinit var session: RuleBuildingSession

    fun selectCase(name: String) {
        case = kb.getCaseByName(name)
        kb.interpret(case)
    }

    infix fun String.replaces(x: String) {
        replaceConclusion(x, this)
    }

    fun condition(c: String) {
        try {
            val condition = ContainsText(textAttribute, c)
            kb.addConditionToCurrentRuleSession(condition)
        } catch (e: Exception) {
            throw Exception(addedConditionBeforeSessionStarted)
        }
    }

    fun condition(i: Int) {
        try {
            kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(numberAttribute, i.toDouble()))
        } catch (e: Exception) {
            throw Exception(addedConditionBeforeSessionStarted)
        }
    }

    fun requireCornerstones(vararg expectedCornerstones: String) {
        kb.conflictingCasesInCurrentRuleSession().map { it.name }.toSet() shouldBe expectedCornerstones.toSet()
    }

    operator fun String.unaryPlus() {
        addConclusion(this)
    }

    fun addConclusion(conclusion: String) {
        val action = ChangeTreeToAddConclusion(Conclusion(conclusion))
        kb.startRuleSession(case, action)
    }

    operator fun String.unaryMinus() {
        removeConclusion(this)
    }

    fun removeConclusion(conclusion: String) {
        val action = ChangeTreeToRemoveConclusion(Conclusion(conclusion))
        kb.startRuleSession(case, action)
    }

    fun replaceConclusion(conclusion: String, replacement: String) {
        val action = ChangeTreeToReplaceConclusion(Conclusion(conclusion), Conclusion(replacement))
        kb.startRuleSession(case, action)
    }

    fun commit() {
        kb.commitCurrentRuleSession()
    }
}
