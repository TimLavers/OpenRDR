package io.rippledown.persistence

import io.rippledown.model.Attribute

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

    override fun load(attributes: Set<Attribute>) {
        require(attributeSet.isEmpty()) {
            "Cannot load attributes into a non-empty attribute store."
        }
        attributeSet.addAll(attributes)
    }
}