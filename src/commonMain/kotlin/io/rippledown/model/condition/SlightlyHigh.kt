package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class SlightlyHigh(override val id: Int? = null, val attribute: Attribute, val allowablePercentageAboveHighRangeCutoff: Int) : Condition() {
    init {
        require(allowablePercentageAboveHighRangeCutoff in 1..99) {
            "Cutoff should be an integer in the range [1, 99]."
        }
    }
    private val fraction = ((100 + allowablePercentageAboveHighRangeCutoff).toDouble())/100.0

    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false

        val realValue = latest.value.real ?: return false

        val normalRange = latest.referenceRange ?: return false

        val rangeCutoff = normalRange.upper ?: return false

        if (realValue <= rangeCutoff) return false

        val cutoff = fraction * rangeCutoff
        if (realValue <= cutoff) return true

        // If the values are very close, return true.
        return abs(realValue - cutoff) < 0.0001
    }

    override fun asText(): String {
        return "${attribute.name} is at most $allowablePercentageAboveHighRangeCutoff% high"
    }

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = SlightlyHigh(id, idToAttribute(attribute.id), allowablePercentageAboveHighRangeCutoff)

    override fun sameAs(other: Condition): Boolean {
        return if (other is SlightlyHigh) {
            other.attribute == attribute && other.allowablePercentageAboveHighRangeCutoff == allowablePercentageAboveHighRangeCutoff
        } else false
    }
}
