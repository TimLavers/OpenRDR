package io.rippledown.kb.scripts

import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToRemoveConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import io.rippledown.persistence.inmemory.InMemoryKB

const val addedConditionBeforeSessionStarted = "Rule session not started."
val text = "Text"
val value = "Value"

fun build(f: BuildTemplate.() -> Unit): BuildTemplate {
    val template = BuildTemplate()
    template.f()
    return template
}

class BuildTemplate {
    private val defaultDate = 1659752689505
    private val kb = KB(InMemoryKB(KBInfo("TestKB")))

    fun cornerstoneCase(name: String, data: String) {
        val caseBuilder = RDRCaseBuilder()
        val textAttribute = kb.attributeManager.getOrCreate(text)
        caseBuilder.addResult(textAttribute, defaultDate, TestResult(data))
        val case = caseBuilder.build(name)
        kb.addCornerstoneCase(case)
    }
    fun case(name: String, data: String) {
        val caseBuilder = RDRCaseBuilder()
        val textAttribute = kb.attributeManager.getOrCreate(text)
        caseBuilder.addResult(textAttribute, defaultDate, TestResult(data))
        val case = caseBuilder.build(name)
        kb.addCase(case)
    }

    fun case(i: Int) {
        val caseBuilder = RDRCaseBuilder()
        val numberAttribute = kb.attributeManager.getOrCreate(value)
        caseBuilder.addResult(numberAttribute, defaultDate, TestResult("$i"))
        val case = caseBuilder.build("$i")
        kb.addCase(case)
    }

    fun cornerstoneCase(i: Int) {
        val caseBuilder = RDRCaseBuilder()
        val numberAttribute = kb.attributeManager.getOrCreate(value)
        caseBuilder.addResult(numberAttribute, defaultDate, TestResult("$i"))
        val case = caseBuilder.build("$i")
        kb.addCornerstoneCase(case)
    }

    fun session(s: SessionTemplate.() -> Unit): SessionTemplate {
        val template = SessionTemplate(kb)
        template.s()
        return template
    }

    fun requireInterpretation(caseName: String, vararg expectedConclusions: String) {
        val case = kb.getCaseByName(caseName)
        kb.interpret(case)
        case.interpretation.conclusions().map { it.text }.toSet() shouldBe expectedConclusions.toSet()
    }
}

class SessionTemplate( val kb: KB) {
    lateinit var case: RDRCase

    fun selectCase(name: String) {
        case = kb.getCaseByName(name)
        kb.interpret(case)
    }

    infix fun String.replaces(x: String) {
        replaceConclusion(x, this)
    }

    fun condition(c: String) {
        val textAttribute = kb.attributeManager.getOrCreate(text)
        val condition = ContainsText(null, textAttribute, c)
        kb.addConditionToCurrentRuleSession(condition)
    }

    fun condition(i: Int) {
        val numberAttribute = kb.attributeManager.getOrCreate(value)
        kb.addConditionToCurrentRuleSession(GreaterThanOrEqualTo(attribute = numberAttribute, d = i.toDouble()))
    }

    fun requireCornerstones(vararg expectedCornerstones: String) {
        kb.conflictingCasesInCurrentRuleSession().map { it.name }.toSet() shouldBe expectedCornerstones.toSet()
    }

    operator fun String.unaryPlus() {
        addConclusion(this)
    }

    private fun addConclusion(conclusion: String) {
        val action = ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate(conclusion))
        kb.startRuleSession(case, action)
    }

    operator fun String.unaryMinus() {
        removeConclusion(this)
    }

    private fun removeConclusion(conclusion: String) {
        val action = ChangeTreeToRemoveConclusion(kb.conclusionManager.getOrCreate(conclusion))
        kb.startRuleSession(case, action)
    }

    private fun replaceConclusion(conclusion: String, replacement: String) {
        val action = ChangeTreeToReplaceConclusion(kb.conclusionManager.getOrCreate(conclusion), kb.conclusionManager.getOrCreate(replacement))
        kb.startRuleSession(case, action)
    }

    fun commit() {
        kb.commitCurrentRuleSession()
    }
}
