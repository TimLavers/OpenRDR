package io.rippledown.persistence

interface AttributeOrderStore {
    fun idToIndex(): Map<Int, Int>
    fun store(id: Int, index: Int)
    fun load(idToIndex: Map<Int,Int>)
}