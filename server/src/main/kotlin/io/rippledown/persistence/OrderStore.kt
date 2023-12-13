package io.rippledown.persistence

interface OrderStore {
    fun idToIndex(): Map<Int, Int>
    fun store(id: Int, index: Int)
    fun load(idToIndex: Map<Int,Int>)
}