package io.rippledown.persistence

class InMemoryAttributeOrderStore: AttributeOrderStore {
    private val idToIndex = mutableMapOf<Int, Int>()

    override fun idToIndex(): Map<Int, Int> {
        return idToIndex.toMap()
    }

    override fun store(id: Int, index: Int) {
        idToIndex[id] = index
    }

    override fun load(idToIndex: Map<Int, Int>) {
        require(this.idToIndex.isEmpty()) {
            "Cannot load data into a non-empty attribute order store."
        }
        this.idToIndex.putAll(idToIndex)
    }
}