package io.rippledown.persistence

import io.rippledown.model.KBInfo

interface PersistentKB {
    fun kbInfo(): KBInfo
    fun attributeStore(): AttributeStore
    fun attributeOrderStore(): OrderStore
    fun conclusionStore(): ConclusionStore
    fun conclusionOrderStore(): OrderStore
    fun conditionStore(): ConditionStore
    fun ruleStore(): RuleStore
    fun caseStore(): CaseStore
}