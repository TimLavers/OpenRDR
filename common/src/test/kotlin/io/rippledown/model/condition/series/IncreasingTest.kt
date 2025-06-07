package io.rippledown.model.condition.series

import io.kotest.matchers.shouldBe
import io.rippledown.model.TestResult
import kotlinx.serialization.json.Json
import kotlin.test.Test

class IncreasingTest {

    @Test
    fun notEnoughNumericalValues() {
        Increasing.evaluate(emptyList()) shouldBe false
        Increasing.evaluate(series("blah")) shouldBe false
        Increasing.evaluate(series("1")) shouldBe false
        Increasing.evaluate(series("1")) shouldBe false
        Increasing.evaluate(series("1", "")) shouldBe false
        Increasing.evaluate(series("", "2")) shouldBe false
        Increasing.evaluate(series("one", "2")) shouldBe false
        Increasing.evaluate(series("", "2")) shouldBe false
        Increasing.evaluate(series("", "what", "2")) shouldBe false
    }

    @Test
    fun increasing() {
        Increasing.evaluate(series("1", "2")) shouldBe true
        Increasing.evaluate(series("100.0", "101.0")) shouldBe true
        Increasing.evaluate(series("10", "110", "200")) shouldBe true
        Increasing.evaluate(series("10", "110", "200")) shouldBe true
        Increasing.evaluate(series("1.234", "1.664", "1.899")) shouldBe true
        Increasing.evaluate(series("1.234", "1.664", "1.899", "2.001")) shouldBe true
        Increasing.evaluate(series("-1.2", "-1.1", "-1.0")) shouldBe true
    }

    @Test
    fun notIncreasing() {
        Increasing.evaluate(series("1", "0")) shouldBe false
        Increasing.evaluate(series("1", "0", "2")) shouldBe false
        Increasing.evaluate(series("100.0", "100.0")) shouldBe false
        Increasing.evaluate(series("10", "0", "200")) shouldBe false
        Increasing.evaluate(series("10", "110", "200", "199")) shouldBe false
        Increasing.evaluate(series("10", "110", "200", "200")) shouldBe false
    }

    @Test
    fun onTrend() {
        Increasing.onTrend(10.0, 12.0) shouldBe false
        Increasing.onTrend(10.0, 10.0) shouldBe false
        Increasing.onTrend(12.0, 10.0) shouldBe true
    }

    @Test
    fun skipNonNumericValues() {
        Increasing.evaluate(series("10", "", "200")) shouldBe true
        Increasing.evaluate(series("10", "missing", "200")) shouldBe true
        Increasing.evaluate(series("10", "110", "")) shouldBe true
    }

    @Test
    fun evaluateWithEqualNumbers() {
        Increasing.evaluate(series("10", "110", "110")) shouldBe false
    }

    @Test
    fun description() {
        Increasing.description("TSH") shouldBe "TSH increasing"
    }

    @Test
    fun serialization() {
        serializeDeserialize(Increasing) shouldBe Increasing
    }

    private fun series(vararg values: String) = values.map { TestResult(it) }

    fun serializeDeserialize(predicate: SeriesPredicate): SeriesPredicate {
        val serialized = Json.encodeToString(predicate)
        return Json.decodeFromString(serialized)
    }
}