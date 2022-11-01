package io.rippledown.model.caseview

import kotlinx.serialization.Serializable
import io.rippledown.model.Attribute

@Serializable
data class CaseViewProperties(val attributeIndexes: Map<Attribute, Int>) {
    fun orderAttributes(attributes: Set<Attribute>): List<Attribute> {
        return attributes.sortedWith(comparator = compareBy { a -> a.name })
    }
}