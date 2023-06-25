package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import kotlin.test.Test

internal class RuleBuildingSessionTest : RuleTestBase() {
    private val caseA = clinicalNotesCase("a")
    private val addAction = ChangeTreeToAddConclusion(Conclusion(3, "A"))
    private val ruleFactory = DummyRuleFactory()

    @Test
    fun a_session_should_present_no_cornerstones_if_there_are_none() {
        val session = RuleBuildingSession(ruleFactory, RuleTree(), caseA,  addAction, listOf())
        session.cornerstoneCases() shouldBe emptySet()
    }

    @Test
    fun a_session_should_not_present_the_current_case_as_a_cornerstone() {
        val cornerstones = mutableListOf(caseA)
        val session = RuleBuildingSession(ruleFactory, RuleTree(), caseA,  addAction, cornerstones)
        session.cornerstoneCases() shouldBe emptySet()
    }
}