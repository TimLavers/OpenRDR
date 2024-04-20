package io.rippledown.dragdrop

fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to)
        return
    val element = this.removeAt(from) ?: return
    this.add(to, element)
}