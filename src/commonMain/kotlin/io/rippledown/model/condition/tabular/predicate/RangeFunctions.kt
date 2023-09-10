package io.rippledown.model.condition.tabular.predicate

import io.rippledown.model.TestResult

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data object Low: TestResultPredicate {
    override fun evaluate(result: TestResult) = result.isLow()

    override fun description(plural: Boolean) = "${isOrAre(plural)} low"
}

@Serializable
data object Normal: TestResultPredicate {
    override fun evaluate(result: TestResult) = result.isNormal()

    override fun description(plural: Boolean) = "${isOrAre(plural)} normal"
}

@Serializable
data object High: TestResultPredicate {
    override fun evaluate(result: TestResult) = result.isHigh()

    override fun description(plural: Boolean) = "${isOrAre(plural)} high"
}

@Serializable
data class AtMostPercentageHigh(val allowablePercentageAboveHighRangeCutoff: Int): TestResultPredicate{
    init {
        require(allowablePercentageAboveHighRangeCutoff in 1..99) {
            "Cutoff should be an integer in the range [1, 99]."
        }
    }
    private val fraction = ((100 + allowablePercentageAboveHighRangeCutoff).toDouble())/100.0

    override fun evaluate(result: TestResult): Boolean {
        val realValue = result.value.real ?: return false

        val normalRange = result.referenceRange ?: return false

        val rangeCutoff = normalRange.upper ?: return false

        if (realValue <= rangeCutoff) return false

        val cutoff = fraction * rangeCutoff
        if (realValue <= cutoff) return true

        // If the values are very close, return true.
        return abs(realValue - cutoff) < 0.0001
    }

    override fun description(plural: Boolean) = "${isOrAre(plural)} at most $allowablePercentageAboveHighRangeCutoff% high"
}

@Serializable
data class AtMostPercentageLow(val allowablePercentageBelowLowRangeCutoff: Int): TestResultPredicate {
    init {
        require(allowablePercentageBelowLowRangeCutoff in 1..99) {
            "Cutoff should be an integer in the range [1, 99]."
        }
    }
    private val fraction = ((100 - allowablePercentageBelowLowRangeCutoff).toDouble())/100.0

    override fun evaluate(result: TestResult): Boolean {
        val realValue = result.value.real ?: return false

        val normalRange = result.referenceRange ?: return false

        val rangeCutoff = normalRange.lower ?: return false

        if (realValue >= rangeCutoff) return false

        val cutoff = fraction * rangeCutoff
        if (realValue >= cutoff) return true

        // If the values are very close, return true.
        return abs(realValue - cutoff) < 0.0001
    }

    override fun description(plural: Boolean) = "${isOrAre(plural) } at most $allowablePercentageBelowLowRangeCutoff% low"
}