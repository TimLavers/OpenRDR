package io.rippledown.persistence

import io.rippledown.model.KBInfo

class InMemoryKB(val kbInfo: KBInfo): PersistentKB {

    override fun kbInfo(): KBInfo {
        return kbInfo
    }
}