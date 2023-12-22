package io.rippledown.model.condition.episodic.predicate

import io.rippledown.model.TestResult
import kotlinx.serialization.Serializable

@Serializable
sealed interface TestResultPredicate {
    fun evaluate(result: TestResult): Boolean
    fun description(plural: Boolean): String
}