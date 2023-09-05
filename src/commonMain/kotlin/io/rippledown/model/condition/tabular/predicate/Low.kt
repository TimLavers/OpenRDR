package io.rippledown.model.condition.tabular.predicate

import io.rippledown.model.TestResult

import kotlinx.serialization.Serializable

@Serializable
class Low: TestResultPredicate {
    override fun evaluate(result: TestResult) = result.isLow()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return !(other == null || this::class != other::class)
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}