package io.rippledown.model.condition.structural

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class IsPresentInCase(val attribute: Attribute): CaseStructurePredicate {
    override fun evaluate(case: RDRCase) = attribute in case.attributes
    override fun description() = "${attribute.name} is in case"
    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = IsPresentInCase(idToAttribute(attribute.id))
    override fun attributeNames() = setOf(attribute.name)
}

@Serializable
data class IsAbsentFromCase(val attribute: Attribute): CaseStructurePredicate {
    override fun evaluate(case: RDRCase) = !case.attributes.contains(attribute)
    override fun description() = "${attribute.name} is not in case"
    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = IsAbsentFromCase(idToAttribute(attribute.id))
    override fun attributeNames() = setOf(attribute.name)
}