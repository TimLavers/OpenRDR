package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

/**
 * The conjunction of two conditions. Required for technical reasons,
 * not meant to be used in rules.
 */
@Serializable
data class And(val left: Condition, val right: Condition): Condition() {
    override val id = null

    override fun holds(case: RDRCase) = left.holds(case) && right.holds(case)

    override fun asText() = "${left.asText()} and ${right.asText()}"

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = And(left.alignAttributes(idToAttribute), right.alignAttributes(idToAttribute))

    override fun sameAs(other: Condition) = when(other) {
            is And -> left.sameAs(other.left) && right.sameAs(other.right)
            else -> false
        }

    override fun attributeNames() = left.attributeNames() + right.attributeNames()
}