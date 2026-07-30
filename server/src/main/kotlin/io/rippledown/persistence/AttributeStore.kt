package io.rippledown.persistence

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind

interface AttributeStore {
    fun all(): Set<Attribute>
    fun create(name: String, kind: AttributeKind = AttributeKind.EXTERNAL): Attribute
    fun store(attribute: Attribute)
    fun load(attributes: Set<Attribute>)
}