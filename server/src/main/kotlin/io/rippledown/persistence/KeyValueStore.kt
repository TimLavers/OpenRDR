package io.rippledown.persistence

data class KeyValue(val id: Int, val key: String, val value: String) {
    init {
        val pattern = "[A-Za-z0-9_]+"
        require(pattern.toRegex().matches(key)) {
            "Key must match $pattern."
        }
    }
}

interface KeyValueStore {
    fun all(): Set<KeyValue>
    fun create(key: String, value: String): KeyValue
    fun store(keyValue: KeyValue)
    fun load(data: Set<KeyValue>)
    fun containsKey(key: String) = get(key) != null
    fun get(key: String) = all().find { it.key == key }
}