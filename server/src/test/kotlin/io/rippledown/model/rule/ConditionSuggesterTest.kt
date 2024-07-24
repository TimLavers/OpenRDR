package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.rippledown.model.Attribute
import io.rippledown.model.TestResult
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.isAbsent
import io.rippledown.model.condition.isPresent
import kotlin.test.Test

internal class ConditionSuggesterTest {
    val a = Attribute(0, "A")
    val b = Attribute(1, "B")

    @Test
    fun `single attribute`() {
        val sessionCase = case(a to "1")
        val suggester = ConditionSuggester(setOf(a), sessionCase)
        val suggestions = suggester.suggestions()

        checkContainsCondition(suggestions, isPresent(a))
    }

    @Test
    fun `two attributes, one in case`() {
        val sessionCase = case(a to "1")
        val suggester = ConditionSuggester(setOf(a, b), sessionCase)
        val suggestions = suggester.suggestions()

        checkContainsCondition(suggestions, isPresent(a))
        checkContainsCondition(suggestions, isAbsent(b))
    }

    @Test
    fun `two attributes, both in case`() {
        val sessionCase = case(a to "1", b to "2")
        val suggester = ConditionSuggester(setOf(a, b), sessionCase)
        val suggestions = suggester.suggestions()

        checkContainsCondition(suggestions, isPresent(a))
        checkContainsCondition(suggestions, isPresent(b))
    }

    @Test
    fun createCondition() {
        val sessionCase = case(a to "1", b to "2")
        with (ConditionSuggester(setOf(a, b), sessionCase)) {
            val p = this.predicates(TestResult("5.1"))
            p shouldHaveSize 4
            p shouldContain GreaterThanOrEquals(5.1)
            p shouldContain LessThanOrEquals(5.1)
            p shouldContain Is("5.1")
            p shouldContain Contains("5.1")
        }
    }

    private fun checkContainsCondition(conditions: Collection<Condition>, expected: Condition) {
        var found = false
        conditions.forEach {
            if (expected.sameAs(it)) {
                found = true
            }
        }
        require(found) {
            "Did not find: $expected in $conditions"
        }
    }
}