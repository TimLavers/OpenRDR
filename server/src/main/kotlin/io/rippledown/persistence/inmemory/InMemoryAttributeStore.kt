package io.rippledown.persistence.inmemory

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.persistence.AttributeStore

class InMemoryAttributeStore(attributes: Set<Attribute>): AttributeStore {
    constructor(): this(emptySet())
    private val attributeSet = mutableSetOf<Attribute>()

    init {
        attributeSet.addAll(attributes)
    }

    override fun all(): Set<Attribute> {
        return attributeSet
    }

    override fun store(attribute: Attribute) {
        attributeSet.add(attribute)
    }

    override fun create(name: String, kind: AttributeKind): Attribute {
        val maxById = attributeSet.maxByOrNull { it.id }
        val maxId = maxById?.id ?: 0
        val newAttribute = Attribute(maxId + 1, name, kind)
        attributeSet.add(newAttribute)
        return newAttribute
    }

    override fun load(attributes: Set<Attribute>) {
        require(attributeSet.isEmpty()) {
            "Cannot load attributes into a non-empty attribute store."
        }
        attributeSet.addAll(attributes)
    }
}