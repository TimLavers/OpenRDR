package io.rippledown.persistence

data class KeyValue(val id: Int, val key: String, val value: String)

interface KeyValueStore {
    fun all(): Set<KeyValue>
    fun create(key: String, value: String): KeyValue
    fun store(keyValue: KeyValue)
    fun load(data: Set<KeyValue>)
}