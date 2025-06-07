package io.rippledown.model.condition.series

import io.rippledown.model.TestResult
import kotlinx.serialization.Serializable

@Serializable
sealed class Trend: SeriesPredicate {
    override fun evaluate(testResults: List<TestResult>): Boolean {
        val numericalValues = testResults.mapNotNull { it.value.real }
        val length = numericalValues.size
        if (length < 2) return false
        for (i in 1..<length) {
            if (!onTrend(numericalValues[i], numericalValues[i-1])) return false
        }
        return true
    }

    abstract fun onTrend(current: Double, previous: Double): Boolean

}
@Serializable
data object Increasing: Trend() {
    override fun onTrend(current: Double, previous: Double) = current > previous

    override fun description(attributeName: String) = "$attributeName increasing"
}
@Serializable
data object Decreasing: Trend() {
    override fun onTrend(current: Double, previous: Double) = current < previous

    override fun description(attributeName: String) = "$attributeName decreasing"
}