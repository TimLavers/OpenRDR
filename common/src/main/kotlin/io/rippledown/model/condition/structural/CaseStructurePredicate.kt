package io.rippledown.model.condition.structural

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
sealed interface CaseStructurePredicate {
    fun evaluate(case: RDRCase): Boolean
    fun description(): String
    fun alignAttributes(idToAttribute: (Int) -> Attribute): CaseStructurePredicate
    fun attributeNames(): Collection<String> = emptySet()
}