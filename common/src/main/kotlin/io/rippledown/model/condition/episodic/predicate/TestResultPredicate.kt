package io.rippledown.model.condition.episodic.predicate

import io.rippledown.model.Result
import kotlinx.serialization.Serializable

@Serializable
sealed interface TestResultPredicate {
    fun evaluate(result: Result): Boolean
    fun description(plural: Boolean): String
}