package io.rippledown.kb.scripts

import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.RuleSessionManager
import io.rippledown.kb.commentsFor
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.Result
import io.rippledown.model.condition.containsText
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.persistence.inmemory.InMemoryKB

const val addedConditionBeforeSessionStarted = "Rule session not started."
const val text = "Text"
const val value = "Value"

fun build(f: BuildTemplate.() -> Unit): BuildTemplate {
    val template = BuildTemplate()
    template.f()
    return template
}

class BuildTemplate {
    private val defaultDate = 1659752689505
    private val kb = KB(InMemoryKB(KBInfo("TestKB")))
    private val session = KBSession(kb)
    private val rsm = session.ruleSessionManager

    fun cornerstoneCase(name: String, data: String) {
        val caseBuilder = RDRCaseBuilder()
        val textAttribute = kb.attributeManager.getOrCreate(text)
        caseBuilder.addResult(textAttribute, defaultDate, Result(data))
        val case = caseBuilder.build(name)
        kb.addCornerstoneCase(case)
    }

    fun case(name: String, data: String) {
        val caseBuilder = RDRCaseBuilder()
        val textAttribute = kb.attributeManager.getOrCreate(text)
        caseBuilder.addResult(textAttribute, defaultDate, Result(data))
        val case = caseBuilder.build(name)
        kb.addProcessedCase(case)
    }

    fun case(i: Int) {
        val caseBuilder = RDRCaseBuilder()
        val numberAttribute = kb.attributeManager.getOrCreate(value)
        caseBuilder.addResult(numberAttribute, defaultDate, Result("$i"))
        val case = caseBuilder.build("$i")
        kb.addProcessedCase(case)
    }

    fun cornerstoneCase(i: Int) {
        val caseBuilder = RDRCaseBuilder()
        val numberAttribute = kb.attributeManager.getOrCreate(value)
        caseBuilder.addResult(numberAttribute, defaultDate, Result("$i"))
        val case = caseBuilder.build("$i")
        kb.addCornerstoneCase(case)
    }

    fun session(s: SessionTemplate.() -> Unit): SessionTemplate {
        val template = SessionTemplate(kb, rsm)
        template.s()
        return template
    }

    fun requireInterpretation(caseName: String, vararg expectedComments: String) {
        val case = kb.getProcessedCaseByName(caseName)
        kb.commentsFor(case) shouldBe expectedComments.toSet()
    }

    fun undoLastRuleSession() {
        rsm.undoLastRuleSession()
    }
}

class SessionTemplate(val kb: KB, val rsm: RuleSessionManager) {
    lateinit var case: RDRCase

    fun selectCase(name: String) {
        case = kb.getProcessedCaseByName(name)
        kb.interpret(case)
    }

    infix fun String.replaces(x: String) {
        replaceComment(x, this)
    }

    fun condition(c: String) {
        val textAttribute = kb.attributeManager.getOrCreate(text)
        val condition = containsText(null, textAttribute, c)
        rsm.addConditionToCurrentRuleSession(condition)
    }

    fun condition(i: Int) {
        val numberAttribute = kb.attributeManager.getOrCreate(value)
        rsm.addConditionToCurrentRuleSession(greaterThanOrEqualTo(attribute = numberAttribute, d = i.toDouble()))
    }

    fun requireCornerstones(vararg expectedCornerstones: String) {
        rsm.conflictingCasesInCurrentRuleSession().map { it.name }.toSet() shouldBe expectedCornerstones.toSet()
    }

    operator fun String.unaryPlus() {
        rsm.startRuleSessionToAddComment(case, this)
    }

    operator fun String.unaryMinus() {
        rsm.startRuleSessionToRemoveComment(case, this)
    }

    private fun replaceComment(comment: String, replacement: String) {
        rsm.startRuleSessionToReplaceComment(case, comment, replacement)
    }

    fun commit() {
        rsm.commitCurrentRuleSession()
    }
}
