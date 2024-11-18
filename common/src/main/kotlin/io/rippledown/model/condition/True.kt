package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

/**
 * A condition that is always true. Required for technical reasons,
 * not meant to be used in rules.
 */
@Serializable
data object True: Condition() {
    override val id = null

    override fun holds(case: RDRCase) = true

    override fun asText() = "TRUE"

    override fun userExpression() = ""

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = this

    override fun sameAs(other: Condition) = other == this
}