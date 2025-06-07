package io.rippledown.model.condition.episodic.predicate

import io.rippledown.model.TestResult
import kotlinx.serialization.Serializable

@Serializable
data class GreaterThanOrEquals(val d: Double) : TestResultPredicate {
    override fun evaluate(result: TestResult): Boolean {
        val real = result.value.real ?: return false
        return real >= d
    }

    override fun description(plural: Boolean) = "≥ $d"
}

@Serializable
data class GreaterThan(val d: Double) : TestResultPredicate {
    override fun evaluate(result: TestResult): Boolean {
        val real = result.value.real ?: return false
        return real > d
    }

    override fun description(plural: Boolean) = "> $d"
}

@Serializable
data class LessThanOrEquals(val d: Double) : TestResultPredicate {
    override fun evaluate(result: TestResult): Boolean {
        val real = result.value.real ?: return false
        return real <= d
    }

    override fun description(plural: Boolean) = "≤ $d"
}

@Serializable
data class LessThan(val d: Double) : TestResultPredicate {
    override fun evaluate(result: TestResult): Boolean {
        val real = result.value.real ?: return false
        return real < d
    }

    override fun description(plural: Boolean) = "< $d"
}
