package io.rippledown.persistence.inmemory

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB

class InMemoryKB(val kbInfo: KBInfo): PersistentKB {

    private val attributeStore = InMemoryAttributeStore()
    private val attributeOrderStore = InMemoryAttributeOrderStore()
    private val conclusionStore = InMemoryConclusionStore()
    private val conditionStore = InMemoryConditionStore()
    private val ruleStore = InMemoryRuleStore()
    private val cornerstoneCasesStore = InMemoryCaseStore()

    override fun kbInfo() = kbInfo

    override fun attributeStore() = attributeStore

    override fun attributeOrderStore() = attributeOrderStore

    override fun conclusionStore() = conclusionStore

    override fun conditionStore() = conditionStore

    override fun ruleStore() = ruleStore

    override fun caseStore() = cornerstoneCasesStore
}