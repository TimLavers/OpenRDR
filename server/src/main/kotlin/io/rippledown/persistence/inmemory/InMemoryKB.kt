package io.rippledown.persistence.inmemory

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB

class InMemoryKB(kbInfo: KBInfo) : PersistentKB {

    private var kbInfo = kbInfo

    private val attributeStore = InMemoryAttributeStore()
    private val attributeOrderStore = InMemoryOrderStore()
    private val derivedDefinitionStore = InMemoryDerivedDefinitionStore()
    private val conditionStore = InMemoryConditionStore()
    private val ruleStore = InMemoryRuleStore()
    private val ruleSessionRecordStore = InMemoryRuleSessionRecordStore()
    private val cornerstoneCasesStore = InMemoryCaseStore()
    private val metaDataStore = InMemoryKeyValueStore()

    override fun kbInfo() = kbInfo

    override fun rename(newName: String) {
        kbInfo = KBInfo(kbInfo.id, newName)
    }

    override fun attributeStore() = attributeStore

    override fun attributeOrderStore() = attributeOrderStore

    override fun derivedDefinitionStore() = derivedDefinitionStore

    override fun conditionStore() = conditionStore

    override fun ruleStore() = ruleStore

    override fun caseStore() = cornerstoneCasesStore

    override fun metaDataStore() = metaDataStore

    override fun ruleSessionRecordStore() = ruleSessionRecordStore
}