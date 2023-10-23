package io.rippledown.model.condition.series

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class DecreasingTest {

    @Test
    fun notEnoughNumericalValues() {
        Decreasing.evaluate(emptyList()) shouldBe false
        Decreasing.evaluate(series("blah")) shouldBe false
        Decreasing.evaluate(series("1")) shouldBe false
        Decreasing.evaluate(series("1")) shouldBe false
        Decreasing.evaluate(series("1", "")) shouldBe false
        Decreasing.evaluate(series("", "2")) shouldBe false
        Decreasing.evaluate(series("one", "2")) shouldBe false
        Decreasing.evaluate(series("", "2")) shouldBe false
        Decreasing.evaluate(series("", "what", "2")) shouldBe false
    }

    @Test
    fun decreasing() {
        Decreasing.evaluate(series("10", "2")) shouldBe true
        Decreasing.evaluate(series("102.0", "101.0")) shouldBe true
        Decreasing.evaluate(series("120", "110", "100")) shouldBe true
        Decreasing.evaluate(series("-1.2", "-1.3", "-1.4", "-1.5")) shouldBe true
    }

    @Test
    fun onTrend() {
        Decreasing.onTrend(10.0F, 12.0F) shouldBe true
        Decreasing.onTrend(10.0F, 10.0F) shouldBe false
        Decreasing.onTrend(12.0F, 10.0F) shouldBe false
    }

    @Test
    fun description() {
        Decreasing.description("TSH") shouldBe "TSH decreasing"
    }

    @Test
    fun serialization() {
        serializeDeserialize(Decreasing) shouldBe Decreasing
    }

    private fun series(vararg values: String) = values.map { TestResult(it) }

    fun serializeDeserialize(predicate: SeriesPredicate): SeriesPredicate {
        val serialized = Json.encodeToString(predicate)
        return Json.decodeFromString(serialized)
    }
}