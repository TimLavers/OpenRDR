package io.rippledown.kb.scripts

import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.model.*
import io.rippledown.model.rule.*
import io.rippledown.model.condition.*

const val addedConditionBeforeSessionStarted = "Session not started yet. Please define the case and action before adding a condition"
val textAttribute = Attribute("Text")

fun build(f: BuildTemplate.() -> Unit): BuildTemplate {
    val template = BuildTemplate()
    template.f()
    return template
}

class BuildTemplate {
    val defaultDate = 1659752689505
    private val kb = KB()

    fun case(name: String, data: String) {
        val caseBuilder = RDRCaseBuilder()
        caseBuilder.addResult(textAttribute, defaultDate, TestResult(data))
        val case = caseBuilder.build(name)
        kb.addCornerstone(case)
    }

//    fun case(i: Int) {
//        val case = RDRCase(i.toString(), i.toString())
//        kb.addCornerstone(case)
//    }

    fun session(s: SessionTemplate.() -> Unit): SessionTemplate {
        val template = SessionTemplate(kb)
        template.s()
        return template
    }

    fun requireInterpretation(caseName: String, vararg expectedConclusions: String) {
        val case = kb.getCaseByName(caseName)
        val interpretation = kb.interpret(case)
        interpretation.conclusions() shouldBe expectedConclusions.map { Conclusion(it) }.toSet()
    }
}

private fun KB.getCaseByName(caseName: String): RDRCase = let {
    cornerstones.first { caseName == it.name }
}

class SessionTemplate( val kb: KB) {
    lateinit var case: RDRCase
    lateinit var action: RuleTreeChange
    lateinit var session: RuleBuildingSession

    fun selectCase(name: String) {
        case = kb.getCaseByName(name)
        kb.interpret(case)
    }

    fun condition(c: String) {
        try {
            val condition = ContainsText(textAttribute, c)
            session.addCondition(condition)
        } catch (e: Exception) {
            throw Exception(addedConditionBeforeSessionStarted)
        }
    }
//
//    fun condition(i: Int) {
//        try {
//            session.addCondition(IntegerCondition(i))
//        } catch (e: Exception) {
//            throw Exception(addedConditionBeforeSessionStarted)
//        }
//    }
//
    fun requireCornerstones(vararg expectedCornerstones: String) {
        session.cornerstones.map { it.key.name }.toSet() shouldBe expectedCornerstones.toSet()
    }

    fun addConclusion(conclusion: String) {
        action = ChangeTreeToAddConclusion(Conclusion(conclusion), kb.ruleTree)
        session = kb.startSession(case, action)
    }
//
////    fun removeConclusion(conclusion: String) {
////        action = RemoveAction(Conc(conclusion), kb.tree)
////        session = kb.startSession(case, action)
////    }
////
////    fun replaceConclusion(conclusion: String, replacement: String) {
////        action = ReplaceAction(Conc(conclusion), Conc(replacement), kb.tree)
////        session = kb.startSession(case, action)
////    }
//
    fun commit() {
        session.commit()
    }
}
