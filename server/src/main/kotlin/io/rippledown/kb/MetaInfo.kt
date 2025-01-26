package io.rippledown.kb

import io.rippledown.persistence.KeyValueStore

const val DESCRIPTION_KEY = "DESCRIPTION_KEY"

class MetaInfo(private val keyValueStore: KeyValueStore) {
    init {
        if (!keyValueStore.containsKey(DESCRIPTION_KEY)) {
            keyValueStore.create(DESCRIPTION_KEY, "")
        }
    }

    fun getDescription() = keyValueStore.get(DESCRIPTION_KEY)!!.value

    fun setDescription(description: String) {
        val existing = keyValueStore.get(DESCRIPTION_KEY)!!
        keyValueStore.store(existing.copy(value = description))
    }
}