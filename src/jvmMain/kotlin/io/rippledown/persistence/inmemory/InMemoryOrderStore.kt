package io.rippledown.persistence.inmemory

import io.rippledown.persistence.OrderStore

class InMemoryOrderStore : OrderStore {
    companion object {
        const val ERROR_MESSAGE = "Cannot load data into a non-empty attribute order store."
    }

    private val idToIndex = mutableMapOf<Int, Int>()

    override fun idToIndex(): Map<Int, Int> {
        return idToIndex.toMap()
    }

    override fun store(id: Int, index: Int) {
        idToIndex[id] = index
    }

    override fun load(idToIndex: Map<Int, Int>) {
        require(this.idToIndex.isEmpty()) {
            ERROR_MESSAGE
        }
        this.idToIndex.putAll(idToIndex)
    }
}