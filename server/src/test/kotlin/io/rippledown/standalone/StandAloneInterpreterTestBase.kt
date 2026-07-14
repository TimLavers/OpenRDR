package io.rippledown.standalone

import io.rippledown.kb.KB
import io.rippledown.kb.RuleSessionManager
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.isCondition
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest

const val valueBlah = "blah"
const val valueWhatever = "whatever"
const val valueSuch = "such"

const val COMMENT_1 = "Zebra"
const val COMMENT_2 = "Aardvark"
const val COMMENT_3 = "Rhinoceros"

open class StandAloneInterpreterTestBase {
    lateinit var kb: KB
    lateinit var a: Attribute
    lateinit var b: Attribute
    lateinit var c: Attribute
    lateinit var rsm: RuleSessionManager
    lateinit var interpreter: StandAloneInterpreter
    var caseCount = 0

    open fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "TestKB")))

        // Create the attributes.
        a = kb.attributeManager.getOrCreate("Attribute A")
        b = kb.attributeManager.getOrCreate("Attribute B")
        c = kb.attributeManager.getOrCreate("Attribute C")

        // Add some rules.
        rsm = RuleSessionManager(kb)
        createRuleAddingCommentForValue(a, valueBlah, COMMENT_1)
        createRuleAddingCommentForValue(b, valueWhatever, COMMENT_2)
        createRuleAddingCommentForValue(c, valueSuch, COMMENT_3)

        interpreter = StandAloneInterpreter(kb)
    }

    fun createViewableCase(attribute: Attribute, value: String): ViewableCase {
        val case = createCase(attribute, value)
        return ViewableCase(case, CaseViewProperties(listOf(attribute)))
    }

    fun createCase(attribute: Attribute, value: String): RDRCase {
        caseCount++
        with(RDRCaseBuilder()) {
            addResult(attribute, defaultDate, Result(value))
            return build("Case${caseCount}")
        }
    }

    fun createRuleAddingCommentForValue(attribute: Attribute, value: String, comment: String) {
        val case1 = createViewableCase(attribute, value)
        rsm.startRuleSessionToAddComment(case1, comment)
        rsm.addConditionToCurrentRuleSession(isCondition(null, attribute, value))
        rsm.commitCurrentRuleSession()
    }
}