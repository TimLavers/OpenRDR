package io.rippledown.model.rule

import io.rippledown.model.*
import io.rippledown.model.condition.Condition
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

    private fun case(vararg pairs: Pair<Attribute, String>, name: String = "SessionCase"): RDRCase {
        val builder = RDRCaseBuilder()
        pairs.forEach {
            builder.addValue(it.first, defaultDate, it.second)
        }
        return builder.build(name)
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