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
sealed class ExtendedRangeFunction(private val rangeComparison: ExpandedRangeComparison): TestResultPredicate {
    override fun evaluate(result: TestResult) = rangeComparison.evaluate(result)
}

@Serializable
data class LowByAtMostSomePercentage(val allowablePercentageBelowLowRangeCutoff: Int): ExtendedRangeFunction(AllowSlightlyLow(allowablePercentageBelowLowRangeCutoff)) {
    override fun description(plural: Boolean) = "${isOrAre(plural) } low by at most $allowablePercentageBelowLowRangeCutoff%"
}

@Serializable
data class NormalOrLowByAtMostSomePercentage(val allowablePercentageBelowLowRangeCutoff: Int): ExtendedRangeFunction(AllowNormalOrSlightlyLow(allowablePercentageBelowLowRangeCutoff)) {
    override fun description(plural: Boolean) = "${isOrAre(plural) } normal or low by at most $allowablePercentageBelowLowRangeCutoff%"
}

@Serializable
data class HighByAtMostSomePercentage(val allowablePercentageAboveHighRangeCutoff: Int): ExtendedRangeFunction(AllowSlightlyHigh(allowablePercentageAboveHighRangeCutoff)) {
    override fun description(plural: Boolean) = "${isOrAre(plural)} high by at most $allowablePercentageAboveHighRangeCutoff%"
}

@Serializable
data class NormalOrHighByAtMostSomePercentage(val allowablePercentageAboveHighRangeCutoff: Int): ExtendedRangeFunction(AllowNormalOrSlightlyHigh(allowablePercentageAboveHighRangeCutoff)) {
    override fun description(plural: Boolean) = "${isOrAre(plural)} normal or high by at most $allowablePercentageAboveHighRangeCutoff%"
}

@Serializable
sealed class ExpandedRangeComparison(private val lowerCutoffExpansionPercentage: Int, private val upperCutoffExpansionPercentage: Int) {
    private val upperCutoffExpansion: Double
    private val lowerCutoffExpansion: Double
    init {
        checkIsReasonablePercentage(upperCutoffExpansionPercentage)
        checkIsReasonablePercentage(lowerCutoffExpansionPercentage)
        upperCutoffExpansion = ((100 + upperCutoffExpansionPercentage).toDouble())/100.0
        lowerCutoffExpansion = ((100 - lowerCutoffExpansionPercentage).toDouble())/100.0
    }

    open fun includeNormalResults() = true

    fun evaluate(result: TestResult): Boolean {
        // Get the real value of the test result and the
        // range limits for the result's  normal range,
        // returning false if any of these are null.
        val realValue = result.value.real ?: return false
        val normalRange = result.referenceRange ?: return false
        val rangeUpperCutoff = normalRange.upper ?: return false
        val rangeLowerCutoff = normalRange.lower ?: return false

        // Calculate the upper limit of the expanded range.
        val expandedUpperRange = rangeUpperCutoff * upperCutoffExpansion
        // If the result value is very close to the limit, return true.
        if (veryClose(realValue, expandedUpperRange)) return true
        // If the result is beyond the limit (but not very close), return false.
        if (realValue > expandedUpperRange) return false

        // Calculate the lower limit of the expanded range.
        val expandedLowerRange = rangeLowerCutoff * lowerCutoffExpansion
        // If the result value is very close to this limit, return true.
        if (veryClose(realValue, expandedLowerRange)) return true
        // If the result is less than the limit (but not very close), return false.
        if (realValue < expandedLowerRange) return false

        // At this point, we know that the value is within (or very close to)
        // the extended range.
        if (includeNormalResults()) return true

        return !normalRange.isNormal(result.value)
    }

    private fun veryClose(x: Float, y: Double) = abs(x - y) < 0.0001
}

@Serializable
data class AllowSlightlyLow(val allowedPercentageBelow: Int): ExpandedRangeComparison(allowedPercentageBelow, 0) {
    override fun includeNormalResults() = false
}

@Serializable
data class AllowNormalOrSlightlyLow(val allowedPercentageBelow: Int): ExpandedRangeComparison(allowedPercentageBelow, 0)

@Serializable
data class AllowSlightlyHigh(val allowedPercentageAbove: Int): ExpandedRangeComparison(0, allowedPercentageAbove) {
    override fun includeNormalResults() = false
}

@Serializable
data class AllowNormalOrSlightlyHigh(val allowedPercentageAbove: Int): ExpandedRangeComparison(0, allowedPercentageAbove)