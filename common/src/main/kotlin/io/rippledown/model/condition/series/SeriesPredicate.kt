package io.rippledown.model.condition.series

import io.rippledown.model.Result
import kotlinx.serialization.Serializable

@Serializable
sealed interface SeriesPredicate {
    fun evaluate(testResults: List<Result>): Boolean
    fun description(attributeName: String): String
}