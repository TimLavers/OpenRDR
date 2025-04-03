package io.rippledown.model.rule

import io.rippledown.persistence.RuleSessionRecordStore

class RuleSessionRecorder(val dataSore: RuleSessionRecordStore) {

    fun recordRuleSessionCommitted(rulesAdded: Set<Rule>){
        dataSore.create(RuleSessionRecord(null, 1, rulesAdded.map { it.id }.toSet()))
    }

    fun idsOfRulesAddedInMostRecentSession() = dataSore.lastAdded()

    fun allRuleSessionHistories() = dataSore.all()
}