package io.rippledown.model.condition.series

import io.rippledown.model.TestResult
import kotlinx.serialization.Serializable

@Serializable
sealed interface SeriesPredicate {
    fun evaluate(testResults: List<TestResult>): Boolean
    fun description(attributeName: String): String
}