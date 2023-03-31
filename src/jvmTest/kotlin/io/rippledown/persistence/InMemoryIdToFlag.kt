package io.rippledown.persistence

class InMemoryIdToFlag: PersistentIdToFlag {
    private val idToFlag = mutableMapOf<String,Boolean>()
    override fun data(): Map<String, Boolean> {
        return idToFlag
    }

    override fun add(key: String, value: Boolean) {
        idToFlag[key] = value
    }
}