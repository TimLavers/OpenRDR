package io.rippledown.model.condition.tabular.predicate

import io.rippledown.model.TestResult

import kotlinx.serialization.Serializable

@Serializable
data object Low: TestResultPredicate {
    override fun evaluate(result: TestResult) = result.isLow()

    override fun description(plural: Boolean) = if (plural) "are low" else "is low"
}

@Serializable
data object Normal: TestResultPredicate {
    override fun evaluate(result: TestResult) = result.isNormal()

    override fun description(plural: Boolean) = if (plural) "are normal" else "is normal"
}

@Serializable
data object High: TestResultPredicate {
    override fun evaluate(result: TestResult) = result.isHigh()

    override fun description(plural: Boolean) = if (plural) "are high" else "is high"
}