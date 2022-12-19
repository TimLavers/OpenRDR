package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.condition.LessThanOrEqualTo
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import kotlin.test.Test

class KBTest {

    @Test
    fun viewableInterpretedCase() {
        val kb = KB("Blah")
        val comment = "Coffee time!"
        buildRuleToAddAComment(kb, comment)

        val builder = RDRCaseBuilder()
        builder.addValue("ABC", defaultDate, "10")
        builder.addValue("DEF", defaultDate, "20")
        val case =  builder.build("Case2")

        case.interpretation.textGivenByRules() shouldBe ""
        // Check that it has been interpreted.
        val viewableCase = kb.viewableInterpretedCase(case)
        viewableCase.interpretation.textGivenByRules() shouldBe comment

        // Check that ordering is working by getting the current ordering
        // and changing it, and then getting the case again and checking
        // that the new ordering is applied.
        val attributesInOriginalOrder = viewableCase.attributes()
        kb.caseViewManager.moveJustBelow(attributesInOriginalOrder[0], attributesInOriginalOrder[1])
        val caseAfterMove = kb.viewableInterpretedCase(case)
        caseAfterMove.attributes() shouldBe listOf(attributesInOriginalOrder[1], attributesInOriginalOrder[0])
    }

    @Test
    fun interpretCase() {
        val kb = KB("Blah")
        val comment = "Whatever."
        buildRuleToAddAComment(kb, comment)
        val case = createCase("Case1", "1.0")
        case.interpretation.textGivenByRules() shouldBe ""
        kb.interpret(case)
        case.interpretation.textGivenByRules() shouldBe comment
    }

    private fun buildRuleToAddAComment(kb: KB, comment: String) {
        kb.addCase(createCase("Case1", "1.0"))
        val sessionCase = kb.getCaseByName("Case1")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion(comment)))
        kb.commitCurrentRuleSession()
    }

    @Test
    fun equalsTest() {
        val kb1 = KB("Thyroids")
        val kb2 = KB("Glucose")
        val kb3 = KB("thyroids")
        val kb4 = KB("Thyroids")
        (kb1 == kb2) shouldBe false
        (kb1 == kb3) shouldBe false
        (kb1 == kb4) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        val kb1 = KB("Thyroids")
        val kb2 = KB("Thyroids")
        (kb1.hashCode() == kb2.hashCode()) shouldBe true
    }

    @Test(expected = NoSuchElementException::class)
    fun getCaseByNameWhenNoCases() {
        KB("Blah").getCaseByName("Whatever")
    }

    @Test(expected = NoSuchElementException::class)
    fun getCaseByNameUnknownCase() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1"))
        kb.getCaseByName("Whatever")
    }

    @Test
    fun getCase() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1", "1.2"))
        kb.addCase(createCase("Case2"))
        val retrieved = kb.getCaseByName("Case1")
        retrieved.name shouldBe "Case1"
        retrieved.get("Glucose")!!.value.text shouldBe "1.2"
    }

    @Test
    fun allCases() {
        val kb = KB("Blah")
        kb.allCases() shouldBe emptySet()
        for (i in  1..10) {
            kb.addCase(createCase("Case$i"))
        }
        kb.allCases() shouldHaveSize 10
        for (i in  1..10) {
            val retrieved = kb.getCaseByName("Case$i")
            kb.allCases() shouldContain retrieved
        }
    }

    @Test
    fun addCase() {
        val kb = KB("Blah")
        for (i in  1..10) {
            kb.addCase(createCase("Case$i"))
        }
        for (i in  1..10) {
            val retrieved = kb.getCaseByName("Case$i")
            retrieved.name shouldBe "Case$i"
        }
    }

    @Test
    fun containsCaseWithName() {
        val kb = KB("Blah")
        for (i in  1..10) {
            val caseName = "Case$i"
            kb.containsCaseWithName(caseName) shouldBe false
            kb.addCase(createCase(caseName))
            kb.containsCaseWithName(caseName) shouldBe true
        }
    }

    @Test
    fun cannotAddCaseWithSameNameAsExistingCase() {
        val kb = KB("Blah")
        kb.addCase(createCase("Blah"))
        kb.addCase(createCase("Whatever"))
        shouldThrow<IllegalArgumentException>{
            kb.addCase(createCase("Blah"))
        }.message shouldBe "There is already a case with name Blah in the KB."
    }

    @Test
    fun `rule session must be started for rule session operations`() {
        val kb = KB("Blah")
        val noSessionMessage = "Rule session not started."
        shouldThrow<IllegalStateException>{
            kb.addConditionToCurrentRuleSession(createCondition())
        }.message shouldBe noSessionMessage

        shouldThrow<IllegalStateException>{
            kb.conflictingCasesInCurrentRuleSession()
        }.message shouldBe noSessionMessage

        shouldThrow<IllegalStateException>{
            kb.commitCurrentRuleSession()
        }.message shouldBe noSessionMessage
    }

    @Test
    fun `cannot start a rule session if one is already started`() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1"))
        val sessionCase = kb.getCaseByName("Case1")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion("Whatever.")))
        shouldThrow<IllegalStateException> {
            kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion("Stuff.")))
        }.message shouldBe "Session already in progress."
    }

    @Test
    fun `cannot start a rule session if action is not applicable to session case`() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1", "1.0"))
        kb.addCase(createCase("Case2", "1.0"))
        val sessionCase = kb.getCaseByName("Case1")
        val otherCase = kb.getCaseByName("Case1")
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion("Whatever.")))
        kb.commitCurrentRuleSession()
        kb.interpret(otherCase)
        otherCase.interpretation.textGivenByRules() shouldBe "Whatever." // sanity

        shouldThrow<IllegalStateException> {
            kb.startRuleSession(otherCase, ChangeTreeToAddConclusion(Conclusion("Whatever.")))
        }.message shouldBe "Action ChangeTreeToAddConclusion(toBeAdded=Conclusion(text=Whatever.)) is not applicable to case Case1"
    }

    @Test
    fun startRuleSession() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1"))
        val sessionCase = kb.getCaseByName("Case1")
        kb.interpret(sessionCase)
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion("Whatever.")))
        kb.commitCurrentRuleSession()
        kb.interpret(sessionCase)
        sessionCase.interpretation.textGivenByRules() shouldBe "Whatever."
    }

    @Test
    fun conflictingCases() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1", "1.0"))
        kb.addCase(createCase("Case2", "2.0"))
        val sessionCase = kb.getCaseByName("Case1")
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion("Whatever.")))
        kb.conflictingCasesInCurrentRuleSession().map { rdrCase -> rdrCase.name }.toSet() shouldBe setOf("Case2")
    }

    @Test
    fun addCondition() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1", "1.0"))
        kb.addCase(createCase("Case2", "2.0"))
        val sessionCase = kb.getCaseByName("Case1")
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion("Whatever.")))
        kb.addConditionToCurrentRuleSession(LessThanOrEqualTo(Attribute("Glucose"), 1.2))
        kb.conflictingCasesInCurrentRuleSession().size shouldBe 0
    }

    @Test
    fun commitSession() {
        val kb = KB("Blah")
        kb.addCase(createCase("Case1", "1.0"))
        kb.addCase(createCase("Case2", "2.0"))
        val sessionCase = kb.getCaseByName("Case1")
        val otherCase = kb.getCaseByName("Case2")
        sessionCase.interpretation.textGivenByRules() shouldBe ""
        kb.startRuleSession(sessionCase, ChangeTreeToAddConclusion(Conclusion("Whatever.")))
        kb.interpret(otherCase)
        // Rule not yet added...
        otherCase.interpretation.textGivenByRules() shouldBe ""
        kb.commitCurrentRuleSession()
        // Rule now added...
        kb.interpret(otherCase)
        otherCase.interpretation.textGivenByRules() shouldBe "Whatever."
    }

    private fun createCondition(): GreaterThanOrEqualTo {
        return GreaterThanOrEqualTo(Attribute("ABC"), 5.0)
    }

    private fun createCase(caseName: String, glucoseValue: String = "0.667"): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue("Glucose", defaultDate, glucoseValue)
        return builder1.build(caseName)
    }
}