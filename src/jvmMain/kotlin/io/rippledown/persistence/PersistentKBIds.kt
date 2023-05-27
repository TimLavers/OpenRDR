package io.rippledown.persistence

interface PersistentKBIds {
    fun data(): Map<String,Boolean>
    fun add(key: String, value: Boolean)
    fun remove(key: String)
}