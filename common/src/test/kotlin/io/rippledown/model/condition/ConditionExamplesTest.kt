package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.episodic.predicate.IsNumeric
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.predicate.NormalOrHighByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.model.condition.series.Increasing
import kotlin.test.Test

/**
 * Tests bases on interesting example conditions.
 */
class ConditionExamplesTest: ConditionTestBase() {
    private val sixMonthsAgo = daysAgo(180)
    private val ft4Range = ReferenceRange("10.0", "20.0")
    private val ft3Range = ReferenceRange("3.0", "5.5")
    private val ft4 = Attribute(55, "Free T4")
    private val ft3 = Attribute(58, "Free T3")
    private val twoEpisodeCase = RDRCaseBuilder().run {
        addResult(tsh, today, TestResult(Value("3.6"), tshRange, "mU/L"))
        addResult(ft4, today, TestResult(Value("12"), ft4Range, "pmol/L"))
        addResult(tsh, sixMonthsAgo, TestResult(Value("4.3"), tshRange, "mU/L"))
        addResult(ft4, sixMonthsAgo, TestResult(Value("13"), ft4Range, "pmol/L"))
        build("Case 1.4.18")
    }
    private val case21 = RDRCaseBuilder().run {
        addResult(tsh, today, TestResult(Value("<0.01"), tshRange, "mU/L"))
        addResult(ft4, today, TestResult(Value("16"), ft4Range, "pmol/L"))
        addResult(ft3, today, TestResult(Value("5.5"), ft3Range, "pmol/L"))
        addResult(tsh, sixMonthsAgo, TestResult(Value("<0.01"), tshRange, "mU/L"))
        addResult(ft4, sixMonthsAgo, TestResult(Value("17"), ft4Range, "pmol/L"))
        addResult(ft3, sixMonthsAgo, TestResult(Value("6.1"), ft3Range, "pmol/L"))
        build("Case 1.4.21")
    }
    private val oneEpisodeCase =         RDRCaseBuilder().run {
        addResult(tsh, today, TestResult(Value("3.6"), tshRange, "mU/L"))
        addResult(ft4, today, TestResult(Value("12"), ft4Range, "pmol/L"))
        build("One Episode")
    }

    @Test
    fun allBorderline() {
        val condition = EpisodicCondition(null, tsh, NormalOrHighByAtMostSomePercentage(10), All)

        condition.asText() shouldBe "all ${tsh.name} are normal or high by at most 10%"

        condition.holds(twoEpisodeCase) shouldBe true

        // With just one episode, it would be more natural to say "TSH is normal or high by at most 10%"
        // but we still return true for the condition because it might have originally
        // been set up for a multi-episode case but is still logically true for a single
        // episode case.
        condition.holds(oneEpisodeCase) shouldBe true
    }

    @Test
    fun atLeastTwoNumeric() {
        val condition = EpisodicCondition(null, tsh, IsNumeric, AtLeast(2))
        checkConditionCanBeSerialized(condition)

        condition.asText() shouldBe "at least 2 ${tsh.name} are numeric"

        condition.holds(oneEpisodeCase) shouldBe false

        condition.holds(twoEpisodeCase) shouldBe true
    }

    @Test
    fun increasing() {
        val condition = SeriesCondition(null, ft3, Increasing)
        checkConditionCanBeSerialized(condition)

        condition.asText() shouldBe "Free T3 increasing"

        condition.holds(case21) shouldBe true
        condition.holds(oneEpisodeCase) shouldBe false
        condition.holds(twoEpisodeCase) shouldBe false
    }

    @Test
    fun noFT3IsLow() {
        val condition = EpisodicCondition(null, ft3, Low, No)
        checkConditionCanBeSerialized(condition)

        condition.asText() shouldBe "no Free T3 is low"

        condition.holds(case21) shouldBe true
    }
}