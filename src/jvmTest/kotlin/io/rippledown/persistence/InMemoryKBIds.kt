package io.rippledown.persistence

class InMemoryKBIds: PersistentKBIds {
    private val idToFlag = mutableMapOf<String,Boolean>()

    override fun data(): Map<String, Boolean> = idToFlag

    override fun remove(key: String) {
        idToFlag.remove(key)
    }

    override fun add(key: String, value: Boolean) {
        idToFlag[key] = value
    }
}