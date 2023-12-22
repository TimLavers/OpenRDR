package io.rippledown.persistence.inmemory

import io.rippledown.persistence.VerifiedTextStore

class InMemoryVerifiedTextStore() : VerifiedTextStore {
    private val idToText = mutableMapOf<Long, String>()

    override fun get(id: Long): String? = idToText.get(id)

    override fun put(id: Long, text: String) {
        idToText.put(id, text)
    }
}