package io.rippledown.kb.sample

import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.server.KBEndpoint

open class SampleRuleBuilder(val kbe: KBEndpoint) {

    fun isPresent(attribute: Attribute) = EpisodicCondition(null, attribute, IsNotBlank, Current)
    fun isNotPresent(attribute: Attribute) = CaseStructureCondition(null, IsAbsentFromCase(attribute), "")
    fun isLow(attribute: Attribute) = EpisodicCondition(null, attribute, Low, Current)
    fun isNormal(attribute: Attribute) = EpisodicCondition(null, attribute, Normal, Current)
    fun isHigh(attribute: Attribute) = EpisodicCondition(null, attribute, High, Current)
    fun isCondition(attribute: Attribute, text: String) = EpisodicCondition(null, attribute, Is(text), Current)
    fun containsText(attribute: Attribute, text: String) = EpisodicCondition(null, attribute, Contains(text), Current)
    fun doesNotContainText(attribute: Attribute, text: String) = EpisodicCondition(
        null,
        attribute,
        DoesNotContain(text),
        Current
    )

    fun greaterThanOrEqualTo(attribute: Attribute, d: Double) = EpisodicCondition(
        null,
        attribute,
        GreaterThanOrEquals(d),
        Current
    )

    fun lessThanOrEqualTo(attribute: Attribute, d: Double) = EpisodicCondition(
        null,
        attribute,
        LessThanOrEquals(d),
        Current
    )
    fun slightlyLow(attribute: Attribute, cutoff: Int) =
        EpisodicCondition(null, attribute, LowByAtMostSomePercentage(cutoff), Current)

    fun addCommentForCase(caseName: String, comment: String, vararg conditions: Condition) {
        val case = kbe.kb.getCaseByName(caseName)
        val sessionStartRequest = SessionStartRequest(case.id!!, Addition(comment))
        kbe.startRuleSession(sessionStartRequest)
        addConditionsAndCommitRule(*conditions)
    }

    fun replaceCommentForCase(
        caseName: String,
        toGo: String,
        replacement: String,
        vararg conditions: Condition
    ) {
        val case = kbe.kb.getCaseByName(caseName)
        val sessionStartRequest = SessionStartRequest(case.id!!, Replacement(toGo, replacement))
        kbe.startRuleSession(sessionStartRequest)
        addConditionsAndCommitRule(*conditions)
    }

    fun removeCommentForCase(
        caseName: String,
        toGo: String,
        vararg conditions: Condition
    ) {
        val case = kbe.kb.getCaseByName(caseName)
        val sessionStartRequest = SessionStartRequest(case.id!!, Removal(toGo))
        kbe.startRuleSession(sessionStartRequest)
        addConditionsAndCommitRule(*conditions)
    }

    private fun addConditionsAndCommitRule(vararg conditions: Condition) {
        conditions.forEach {
            kbe.addConditionToCurrentRuleBuildingSession(it)
        }
        kbe.commitCurrentRuleSession()
    }
}