package io.rippledown.persistence

import io.rippledown.model.KBInfo

interface PersistenceProvider {
    fun idStore(): PersistentKBIds
    fun kbPersistence(id: String): PersistentKB
    fun createKBPersistence(kbInfo: KBInfo): PersistentKB
    fun destroyKBPersistence(kbInfo: KBInfo)
}