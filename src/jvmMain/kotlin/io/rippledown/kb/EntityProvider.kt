package io.rippledown.kb

fun interface EntityProvider<T> {
    fun forId(id: Int): T
}