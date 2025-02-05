package io.rippledown.persistence.inmemory

import io.rippledown.persistence.KeyValue
import io.rippledown.persistence.KeyValueStore

class InMemoryKeyValueStore: KeyValueStore {
    private val data = mutableSetOf<KeyValue>()

    override fun all(): Set<KeyValue> = data.toSet()

    override fun create(key: String, value: String): KeyValue {
        get(key)?.let {
            throw IllegalArgumentException("Key $key already exists")
        }
        val maxById = data.maxByOrNull { it.id }
        val newId = (maxById?.id ?: 0) + 1
        val newItem = KeyValue(newId, key, value)
        data.add(newItem)
        return newItem
    }

    override fun store(keyValue: KeyValue) {
        val byKey = get(keyValue.key) ?: throw IllegalArgumentException("Unknown key: ${keyValue.key}")
        if (byKey.id != keyValue.id) {
            throw IllegalArgumentException("Id of new value does not match that of existing item.")
        }
        data.remove(byKey)
        data.add(keyValue)
    }

    override fun load(data: Set<KeyValue>) {
        if (this.data.isNotEmpty()) {
            throw IllegalArgumentException("Load should not be called if there are already items.")
        }
        this.data.addAll(data)
    }
}