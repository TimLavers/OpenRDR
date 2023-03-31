package io.rippledown.persistence

interface PersistentIdToFlag {
    fun data(): Map<String,Boolean>
    fun add(key: String, value: Boolean)
}