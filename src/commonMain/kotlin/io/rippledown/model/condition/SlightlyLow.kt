package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class SlightlyLow(val attribute: Attribute, val allowablePercentageBelowLowRangeCutoff: Int) : Condition() {
    init {
        require(allowablePercentageBelowLowRangeCutoff > 0 && allowablePercentageBelowLowRangeCutoff < 100) {
            "Cutoff should be an integer in the range [1, 99]."
        }
    }
    val fraction = ((100 - allowablePercentageBelowLowRangeCutoff).toDouble())/100.0
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false

        val realValue = latest.value.real ?: return false

        val normalRange = latest.referenceRange ?: return false

        val rangeCutoff = normalRange.lower ?: return false

        if (realValue >= rangeCutoff) return false

        val cutoff = fraction * rangeCutoff
        if (realValue >= cutoff) return true

        // If the values are very close, return true.
        return abs(realValue - cutoff) < 0.0001
    }

    override fun asText(): String {
        return "${attribute.name} is at most $allowablePercentageBelowLowRangeCutoff% low"
    }
}