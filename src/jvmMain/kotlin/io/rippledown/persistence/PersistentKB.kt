package io.rippledown.persistence

import io.rippledown.model.KBInfo

interface PersistentKB {
    fun kbInfo(): KBInfo
    fun attributeStore(): AttributeStore
    fun attributeOrderStore(): AttributeOrderStore
    fun conclusionStore(): ConclusionStore
    fun conditionStore(): ConditionStore
    fun ruleStore(): RuleStore
}