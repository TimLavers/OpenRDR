package io.rippledown.persistence

import io.rippledown.model.KBInfo

class InMemoryPersistenceProvider: PersistenceProvider {
    private val idStore = InMemoryKBIds()
    private val kbStore = mutableMapOf<String,InMemoryKB>()

    override fun idStore(): PersistentKBIds = idStore

    override fun createKBPersistence(kbInfo: KBInfo): PersistentKB {
        idStore.add(kbInfo.id, true)
        val result = InMemoryKB(kbInfo)
        kbStore[kbInfo.id] = result
        return result
    }

    override fun kbPersistence(id: String): PersistentKB {
        return kbStore[id]!!
    }

    override fun destroyKBPersistence(kbInfo: KBInfo) {
        kbStore.remove(kbInfo.id)
        idStore.remove(kbInfo.id)
    }
}