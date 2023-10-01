package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.tabular.chain.All
import io.rippledown.model.condition.tabular.predicate.NormalOrHighByAtMostSomePercentage
import kotlin.test.Test

/**
 * Tests bases on interesting example conditions.
 */
class ConditionExamplesTest: ConditionTestBase() {
    private val sixMonthsAgo = daysAgo(180)
    private val ft4Range = ReferenceRange("10.0", "20.0")
    private val ft4 = Attribute(55, "Free T4")

    @Test
    fun allBorderline() {
        val builder = RDRCaseBuilder()
        builder.addResult(tsh, today, TestResult(Value("3.6"), tshRange, "mU/L"))
        builder.addResult(ft4, today, TestResult(Value("12"), ft4Range, "pmol/L"))
        builder.addResult(tsh, sixMonthsAgo, TestResult(Value("4.3"), tshRange, "mU/L"))
        builder.addResult(ft4, sixMonthsAgo, TestResult(Value("13"), ft4Range, "pmol/L"))
        val case = builder.build("Case 1.4.18")

        val condition = TabularCondition(null, tsh, NormalOrHighByAtMostSomePercentage(10), All)
        condition.holds(case) shouldBe true
    }
}