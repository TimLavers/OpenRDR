package io.rippledown.model.rule

import io.rippledown.persistence.RuleSessionRecordStore

class RuleSessionRecorder(private val dataSore: RuleSessionRecordStore) {

    fun recordRuleSessionCommitted(rulesAdded: Set<Rule>){
//        dataSore.store(rulesAdded.map { it.id }.toSet())
    }

    fun idsOfRulesAddedInMostRecentSession(): RuleSessionRecord? {
        TODO()
//        val idsOfLastRules = dataSore.lastAdded() ?: return null
//        return RuleSessionRecord(idsOfLastRules)
    }

//    fun idsOfAllSessionRules() = dataSore.all().flatten().toSet()
}