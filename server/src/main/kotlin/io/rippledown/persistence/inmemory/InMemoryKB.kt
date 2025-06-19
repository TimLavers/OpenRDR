package io.rippledown.persistence.inmemory

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB
import io.rippledown.persistence.RuleSessionRecordStore

class InMemoryKB(val kbInfo: KBInfo): PersistentKB {

    private val attributeStore = InMemoryAttributeStore()
    private val attributeOrderStore = InMemoryOrderStore()
    private val conclusionStore = InMemoryConclusionStore()
    private val conclusionOrderStore = InMemoryOrderStore()
    private val conditionStore = InMemoryConditionStore()
    private val ruleStore = InMemoryRuleStore()
    private val ruleSessionRecordStore = InMemoryRuleSessionRecordStore()
    private val cornerstoneCasesStore = InMemoryCaseStore()
    private val metaDataStore = InMemoryKeyValueStore()

    override fun kbInfo() = kbInfo

    override fun attributeStore() = attributeStore

    override fun attributeOrderStore() = attributeOrderStore

    override fun conclusionStore() = conclusionStore

    override fun conclusionOrderStore() = conclusionOrderStore

    override fun conditionStore() = conditionStore

    override fun ruleStore() = ruleStore

    override fun caseStore() = cornerstoneCasesStore

    override fun metaDataStore() = metaDataStore

    override fun ruleSessionRecordStore() = ruleSessionRecordStore
}