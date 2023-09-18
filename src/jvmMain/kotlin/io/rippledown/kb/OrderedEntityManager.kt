package io.rippledown.kb

import io.rippledown.persistence.OrderStore

private data class IndexedEntity<T>(val entity: T, val index: Int) : Comparable<IndexedEntity<T>> {
    override fun compareTo(other: IndexedEntity<T>): Int {
        return index.compareTo(other.index)
    }
}

open class OrderedEntityManager<T>(orderStore: OrderStore, entityProvider: EntityProvider<T>) {
    private val entityToIndex = mutableMapOf<T, Int>()

    init {
        orderStore.idToIndex().forEach {
            entityToIndex[entityProvider.forId(it.key)] = it.value
        }
    }

    fun allInOrder(): List<T> {
        val orderSet = mutableListOf<IndexedEntity<T>>()
        entityToIndex.map { IndexedEntity(it.key, it.value) }.toCollection(orderSet)
        return orderSet.sorted().map { it.entity }
    }

    fun set(entitiesInOrder: List<T>) {
        entityToIndex.clear()
        entitiesInOrder.forEachIndexed { index, entity -> entityToIndex[entity] = index }
    }

    fun moveJustBelow(moved: T, target: T) {
        // Do the move by:
        //  - doubling the indices of all attributes
        //  - giving the moved conclusion the index (doubled) of the target, plus one
        //  - using these new indices to get the new sorting order
        //  - rebuild the attribute to index map, using the positions in the new sorting order as the indexes.
        val existingOrderSet = mutableSetOf<IndexedEntity<T>>()
        entityToIndex.map { IndexedEntity(it.key, 2 * it.value) }.toCollection(existingOrderSet)
        existingOrderSet.remove(IndexedEntity(moved, 2 * entityToIndex[moved]!!))
        existingOrderSet.add(IndexedEntity(moved, 2 * entityToIndex[target]!! + 1))
        entityToIndex.clear()
        val newOrderSet = existingOrderSet.toSortedSet()
        val asList = newOrderSet.toList()
        for (i in asList.indices) {
            entityToIndex[asList[i].entity] = i
        }
    }
}