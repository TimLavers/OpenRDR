package io.rippledown.kb

import io.rippledown.persistence.OrderStore

private data class IndexedEntity<T>(val entity: T, val index: Int) : Comparable<IndexedEntity<T>> {
    override fun compareTo(other: IndexedEntity<T>): Int {
        return index.compareTo(other.index)
    }
}

open class OrderedEntityManager<T>(orderStore: OrderStore, entityProvider: EntityProvider<T>) {
    companion object {
        const val UNKNOWN_ENTITY = "Unknown entity: "
        const val MOVED_ENTITY_IS_TARGET = "Moved entity is target entity: "
    }
    private val entityToIndex = mutableMapOf<T, Int>()

    init {
        orderStore.idToIndex().forEach {
            entityToIndex[entityProvider.getById(it.key)] = it.value
        }
    }

    fun allInOrder(): List<T> {
        val orderSet = mutableListOf<IndexedEntity<T>>()
        entityToIndex.map { IndexedEntity(it.key, it.value) }.toCollection(orderSet)
        return orderSet.sorted().map { it.entity }
    }

    /**
     * Regenerate the order from the given list of entities.
     */
    fun set(entitiesInOrder: List<T>) {
        entityToIndex.clear()
        entitiesInOrder.forEachIndexed { index, entity -> entityToIndex[entity] = index }
    }

    fun moveJustBelow(moved: T, target: T) {
        check(entityToIndex.containsKey(moved)) {
            "$UNKNOWN_ENTITY$moved"
        }
        check(entityToIndex.containsKey(target)) {
            "$UNKNOWN_ENTITY$target"
        }
        check(moved != target) {
            "$MOVED_ENTITY_IS_TARGET$moved"
        }
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

    fun inOrder(unordered: Collection<T>): List<T> {
        return unordered
            .map { getOrCreate(it) }
            .sorted()
            .map { it.entity }
    }

    /**
     * Insert the entities into the view ordering, maintaining their relative order if it is consistent with the existing view ordering.
     */
    fun insert(ordered: List<T>) {

    }

    private fun getOrCreate(entity: T): IndexedEntity<T> {
        if (!entityToIndex.containsKey(entity)) {
            entityToIndex[entity] = entityToIndex.size
        }
        return IndexedEntity(entity, entityToIndex[entity]!!)
    }
}

