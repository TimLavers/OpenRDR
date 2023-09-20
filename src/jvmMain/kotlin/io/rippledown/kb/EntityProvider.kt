package io.rippledown.kb

interface EntityProvider<T> {
    fun getById(id: Int): T
    fun getOrCreate(text: String): T
}