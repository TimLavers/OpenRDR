package io.rippledown.persistence

import io.rippledown.model.KBInfo

class InMemoryPersistenceProvider: PersistenceProvider {
    private val idStore = InMemoryIdToFlag()
    private val kbStore = mutableMapOf<String,InMemoryKB>()

    override fun idStore(): PersistentIdToFlag = idStore

    override fun createKB(kbInfo: KBInfo): PersistentKB {
        idStore.add(kbInfo.id, true)
        val result = InMemoryKB(kbInfo)
        kbStore[kbInfo.id] = result
        return result
    }

    override fun kbStore(id: String): PersistentKB {
        return kbStore[id]!!
    }
}