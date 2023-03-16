package io.rippledown.kb

import io.rippledown.model.Attribute

class AttributeManager {
    val nameToAttribute = mutableMapOf<String, Attribute>()

    fun getOrCreate(name: String) : Attribute {
        return nameToAttribute.computeIfAbsent(name) {
            Attribute(name, nameToAttribute.size)
        }
    }

    fun restoreWith(attributes: Set<Attribute>) {
        require(nameToAttribute.isEmpty()) {
            "Cannot restore attributes to a non-empty AttributeManager."
        }

    }

    fun all(): Set<Attribute> {
        return nameToAttribute.values.toSet()
    }
}