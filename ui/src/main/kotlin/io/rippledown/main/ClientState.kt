package io.rippledown.main

import io.rippledown.model.KBInfo

class ClientState : KbState {
    private var kbInfo: KBInfo? = null
    private val listeners: MutableSet<(KBInfo?) -> Unit> = mutableSetOf()

    override fun kbChanged(kbInfo: KBInfo?, notifyListeners: Boolean) {
        this.kbInfo = kbInfo
        if (notifyListeners) {
            listeners.forEach {
                it.invoke(kbInfo)
            }
        }
    }

    override fun currentKB(): KBInfo? {
        return kbInfo
    }

    override fun attachListener(callback: (KBInfo?) -> Unit) {
        listeners.clear()
        listeners.add(callback)
    }
}