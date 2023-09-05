package io.rippledown.model.condition.tabular.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class LowTest {
    private val low = Low()
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueBlank() {
        low.evaluate(TestResult(Value(""), range, units)) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        low.evaluate(TestResult(Value(""), null, units)) shouldBe false
        low.evaluate(TestResult(Value("1.2"), null, units)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        low.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNormal() {
        low.evaluate(TestResult(Value("1.8"), range, units)) shouldBe false
        low.evaluate(TestResult(Value("1.0"), range, units)) shouldBe false
    }

    @Test
    fun valueLow() {
        low.evaluate(TestResult(Value("0.8"), range, units)) shouldBe true
    }

    @Test
    fun valueHigh() {
        low.evaluate(TestResult(Value("5.0"), range, units)) shouldBe false
    }

    @Test
    fun equalsTest() {
        low shouldBe Low()
    }

    @Test
    fun hashCodeTest() {
        low.hashCode() shouldBe Low().hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(low) shouldBe low
    }

    fun serializeDeserialize(testResultPredicate: TestResultPredicate): TestResultPredicate {
        val serialized = Json.encodeToString(testResultPredicate)
        return Json.decodeFromString(serialized)
    }
}