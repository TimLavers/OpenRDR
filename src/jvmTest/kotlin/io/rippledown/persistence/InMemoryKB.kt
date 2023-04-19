package io.rippledown.persistence

import io.rippledown.model.KBInfo

class InMemoryKB(val kbInfo: KBInfo): PersistentKB {

    private val attributeStore = InMemoryAttributeStore()
    private val attributeOrderStore = InMemoryAttributeOrderStore()
    private val conclusionStore = InMemoryConclusionStore()

    override fun kbInfo() = kbInfo

    override fun attributeStore() = attributeStore

    override fun attributeOrderStore() = attributeOrderStore

    override fun conclusionStore() = conclusionStore
}