package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import kotlin.test.Test

internal class RuleBuildingSessionTest : RuleTestBase() {
    private val caseA = clinicalNotesCase("a")
    private val interpA = Interpretation(CaseId("CaseA", "CaseA"))
    private val addAction = ChangeTreeToAddConclusion(Conclusion("A"), RuleTree())

    @Test
    fun a_session_should_present_no_cornerstones_if_there_are_none() {
        val session = RuleBuildingSession(caseA, interpA, addAction, mapOf())
        session.cornerstoneCases() shouldBe emptySet()
    }

    @Test
    fun a_session_should_not_present_the_current_case_as_a_cornerstone() {
        val cornerstones = mutableMapOf(Pair(caseA, interpA))
        val session = RuleBuildingSession(caseA, interpA, addAction, cornerstones)
        session.cornerstoneCases() shouldBe emptySet()
    }
}