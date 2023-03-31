package io.rippledown.persistence

import io.rippledown.model.KBInfo

interface PersistentKB {
    fun kbInfo(): KBInfo
}