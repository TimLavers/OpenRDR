package io.rippledown.persistence

import io.rippledown.model.KBInfo

interface PersistenceProvider {
    fun idStore(): PersistentIdToFlag
    fun kbStore(id: String): PersistentKB
    fun createKB(kbInfo: KBInfo): PersistentKB
}