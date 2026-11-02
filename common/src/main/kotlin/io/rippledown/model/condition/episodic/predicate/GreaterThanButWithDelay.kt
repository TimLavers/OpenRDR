package io.rippledown.model.condition.episodic.predicate

import io.rippledown.model.Result
import kotlinx.serialization.Serializable

/**
 * A test-only predicate: the same comparison as [GreaterThan], but each
 * evaluation is delayed by [delayMillis], so that a search using it runs
 * slowly enough to be stopped, or to overlap with other operations, in tests.
 *
 * Its deliberately dumb name means that no user or suggestion will ever
 * produce it, and the reflective predicate construction used for chat
 * conditions cannot create it (it takes two parameters): only the search
 * condition parser recognises the phrase "X is greater than but with delay Y".
 */
@Serializable
data class GreaterThanButWithDelay(
    val d: Double,
    val delayMillis: Long = DEFAULT_DELAY_MILLIS
) : TestResultPredicate {

    override fun evaluate(result: Result): Boolean {
        Thread.sleep(delayMillis)
        return GreaterThan(d).evaluate(result)
    }

    override fun description(plural: Boolean) = "is greater than but with delay $d"

    companion object {
        const val DEFAULT_DELAY_MILLIS = 500L
    }
}
