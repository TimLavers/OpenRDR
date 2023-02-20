package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase

class CaseViewManager {
    private val attributeToIndex = mutableMapOf<Attribute, Int>()

    fun allAttributesInOrder(): List<Attribute> {
        val orderSet = mutableListOf<IndexedAttribute>()
        attributeToIndex.map { IndexedAttribute(it.key, it.value) }.toCollection(orderSet)
        return orderSet.sorted().map { it.attribute }
    }

    fun setAttributes(attributesInOrder: List<Attribute>) {
        attributeToIndex.clear()
        attributesInOrder.forEachIndexed{index, attribute -> attributeToIndex[attribute] = index}
    }

    fun getViewableCase(case: RDRCase): ViewableCase {
        val attributesSorted = case.attributes.map { getIndexedAttribute(it) }.sorted().toList().map { it.attribute }
        return ViewableCase(case, CaseViewProperties(attributesSorted))
    }

    fun moveJustBelow(moved: Attribute, target: Attribute) {
        check(attributeToIndex.containsKey(moved)) {
            "Unknown attribute: $moved"
        }
        check(attributeToIndex.containsKey(target)) {
            "Unknown attribute: $target"
        }
        check(moved != target) {
            "Moved attribute is target attribute, $moved"
        }
        // Do the move by:
        //  - doubling the indices of all attributes
        //  - giving the moved attribute the index (doubled) of the target, plus one
        //  - using these new indices to get the new sorting order
        //  - rebuild the attribute to index map, using the positions in the new sorting order as the indexes.
        val existingOrderSet = mutableSetOf<IndexedAttribute>()
        attributeToIndex.map { IndexedAttribute(it.key, 2 * it.value) }.toCollection(existingOrderSet)
        existingOrderSet.remove(IndexedAttribute(moved, 2 * attributeToIndex[moved]!!))
        existingOrderSet.add(IndexedAttribute(moved, 2 * attributeToIndex[target]!! + 1))
        attributeToIndex.clear()
        val newOrderSet = existingOrderSet.toSortedSet()
        val asList = newOrderSet.toList()
        for (i in asList.indices) {
            attributeToIndex[asList[i].attribute] = i
        }
    }

    private fun getIndexedAttribute(attribute: Attribute): IndexedAttribute {
        if (!attributeToIndex.containsKey(attribute)) {
            attributeToIndex[attribute] = attributeToIndex.size
        }
        return IndexedAttribute(attribute, attributeToIndex[attribute]!!)
    }
}

internal data class IndexedAttribute(val attribute: Attribute, val index: Int) : Comparable<IndexedAttribute> {
    override fun compareTo(other: IndexedAttribute): Int {
        return this.index.compareTo(other.index)
    }
}