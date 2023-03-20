package io.rippledown.kb

import io.rippledown.model.Attribute

class AttributeManager(attributes: Set<Attribute>) {
    private val nameToAttribute = mutableMapOf<String, Attribute>()

    init {
        attributes.forEach {
            nameToAttribute[it.name] = it
        }
    }

    constructor() : this(emptySet())

    fun getOrCreate(name: String) : Attribute {
        return nameToAttribute.computeIfAbsent(name) {
            Attribute(name, nameToAttribute.size)
        }
    }

    fun all(): Set<Attribute> {
        return nameToAttribute.values.toSet()
    }

    fun getById(id: Int):Attribute {
        return nameToAttribute.values.first { it.id == id }
    }
}