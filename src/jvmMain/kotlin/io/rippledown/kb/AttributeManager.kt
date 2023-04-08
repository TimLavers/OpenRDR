package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.persistence.AttributeStore

class AttributeManager(private val attributeStore: AttributeStore) {
    private val nameToAttribute = mutableMapOf<String, Attribute>()

    init {
        attributeStore.all().forEach {
            nameToAttribute[it.name] = it
        }
    }

    fun getOrCreate(name: String) : Attribute {
        return nameToAttribute.computeIfAbsent(name) {
            val newAttribute = Attribute(name, nameToAttribute.size)
            attributeStore.store(newAttribute)
            newAttribute
        }
    }

    fun all(): Set<Attribute> {
        return nameToAttribute.values.toSet()
    }

    fun getById(id: Int):Attribute {
        return nameToAttribute.values.first { it.id == id }
    }
}