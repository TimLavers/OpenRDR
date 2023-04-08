package io.rippledown.persistence

import io.rippledown.model.Attribute

interface AttributeStore {
    fun all(): Set<Attribute>
    fun store(attribute: Attribute)
    fun load(attributes: Set<Attribute>)
}