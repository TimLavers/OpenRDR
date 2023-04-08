package io.rippledown.persistence

import io.rippledown.model.KBInfo

class InMemoryKB(val kbInfo: KBInfo): PersistentKB {

    private val attributeStore = InMemoryAttributeStore()

    override fun kbInfo() = kbInfo

    override fun attributeStore() = attributeStore
}