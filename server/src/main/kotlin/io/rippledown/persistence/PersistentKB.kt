package io.rippledown.persistence

import io.rippledown.model.KBInfo

interface PersistentKB {
    fun kbInfo(): KBInfo
    fun rename(newName: String)
    fun attributeStore(): AttributeStore
    fun attributeOrderStore(): OrderStore
    fun derivedDefinitionStore(): DerivedDefinitionStore
    fun conditionStore(): ConditionStore
    fun ruleStore(): RuleStore
    fun ruleSessionRecordStore(): RuleSessionRecordStore
    fun caseStore(): CaseStore
    fun metaDataStore(): KeyValueStore
}