package io.rippledown.persistence.inmemory

import io.rippledown.log.lazyLogger
import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.PersistentKB
import io.rippledown.persistence.PersistentKBIds

class InMemoryPersistenceProvider: PersistenceProvider {
    val logger = lazyLogger
    private val idStore = InMemoryKBIds()
    private val kbStore = mutableMapOf<String, InMemoryKB>()
    init {
        logger.info("InMemoryPersistenceProvider created.")
    }

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