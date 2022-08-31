package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlin.test.Test

internal class IsNormalTest: ConditionTestBase() {

    private val condition = IsNormal(tsh)

    @Test
    fun attributeNotInCase() {
        condition.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(tsh.name, defaultDate,"0.667")
        val case1 = builder1.build("Case1")
        condition.holds(case1) shouldBe false
    }

    @Test
    fun valueNormal() {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.667", range, "pmol/L"))
        val case = builder1.build("Case")
        condition.holds(case) shouldBe true
    }

    @Test
    fun valueLow() {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.067", range, "pmol/L"))
        val case = builder1.build("Case")
        condition.holds(case) shouldBe false
    }

    @Test
    fun valueHigh() {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("9.667", range, "pmol/L"))
        val case = builder1.build("Case")
        condition.holds(case) shouldBe false
    }

    @Test
    fun currentValueNormal() {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("0.67"), range, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        val case = builder.build("Case1")

        condition.holds(case) shouldBe true
    }

    @Test
    fun currentValueNotNormal() {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("8.67"), range, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.80"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        val case = builder.build("Case1")

        condition.holds(case) shouldBe false
    }

    @Test
    fun noValueNormal() {
        val builder = RDRCaseBuilder()
        val tshResult1 = TestResult(Value("8.67"), range, "mU/L")
        builder.addResult(tsh.name, defaultDate, tshResult1)
        val range0 = ReferenceRange("0.25", "2.90")
        val tshResult0 = TestResult(Value("0.08"), range0, "mU/L")
        val yesterday = daysAgo(1)
        builder.addResult(tsh.name, yesterday, tshResult0)
        val case = builder.build("Case1")

        condition.holds(case) shouldBe false
    }

    @Test
    fun currentValueBlank() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun asText() {
        condition.asText() shouldBe "TSH is normal"
        IsNormal(Attribute("Blah !@#@#  Blah is normal")).asText() shouldBe "Blah !@#@#  Blah is normal is normal"
    }
}