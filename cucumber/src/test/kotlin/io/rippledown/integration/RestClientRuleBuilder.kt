package io.rippledown.integration

import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.structural.IsAbsentFromCase

open class RestClientRuleBuilder(val restClient: RESTClient) {
    val attributeFactory = RestClientAttributeFactory(restClient)
    val conclusionFactory = RestClientConclusionFactory(restClient)
    val conditionFactory = RestClientConditionFactory(restClient)

    fun isPresent(attribute: Attribute) = EpisodicCondition(null, attribute, IsNotBlank, Current)
    fun isNotPresent(attribute: Attribute) = CaseStructureCondition(null, IsAbsentFromCase(attribute))
    fun isLow(attribute: Attribute) = EpisodicCondition(null, attribute, Low, Current)
    fun isNormal(attribute: Attribute) = EpisodicCondition(null, attribute, Normal, Current)
    fun isHigh(attribute: Attribute) = EpisodicCondition(null, attribute, High, Current)
    fun isCondition(attribute: Attribute, text: String) = EpisodicCondition(null, attribute, Is(text), Current)
    fun containsText(attribute: Attribute, text: String) = EpisodicCondition(null, attribute, Contains(text), Current)
    fun doesNotContainText(attribute: Attribute, text: String) = EpisodicCondition(null, attribute, DoesNotContain(text), Current)
    fun greaterThanOrEqualTo(attribute: Attribute, d: Double) = EpisodicCondition(null, attribute, GreaterThanOrEquals(d), Current)
    fun lessThanOrEqualTo(attribute: Attribute, d: Double) = EpisodicCondition(null, attribute, LessThanOrEquals(d), Current)
    fun slightlyLow(attribute: Attribute, cutoff: Int) =
        EpisodicCondition(null, attribute, LowByAtMostSomePercentage(cutoff), Current)

    fun addCommentForCase(caseName: String, comment: String, vararg conditions: Condition) {
        restClient.getCaseWithName(caseName)
        val conclusion = conclusionFactory.getOrCreate(comment)
        restClient.startSessionToAddConclusionForCurrentCase(conclusion)
        conditions.forEach {
            try {
                restClient.addConditionForCurrentSession(it)
            } catch (e: Throwable) {
                println("Could not add condition: $it")
                throw e
            }
        }
        restClient.commitCurrentSession()
    }

    fun replaceCommentForCase(
        caseName: String,
        toGo: String,
        replacement: String,
        vararg conditions: Condition
    ) {
        restClient.getCaseWithName(caseName)
        val conclusionToGo = conclusionFactory.getOrCreate(toGo)
        val replacementConclusion = conclusionFactory.getOrCreate(replacement)
        restClient.startSessionToReplaceConclusionForCurrentCase(conclusionToGo, replacementConclusion)
        conditions.forEach {
            restClient.addConditionForCurrentSession(it)
        }
        restClient.commitCurrentSession()
    }

}