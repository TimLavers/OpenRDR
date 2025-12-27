package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionTipTest {

    private lateinit var conditionTip: ConditionTip
    private lateinit var glucose: Attribute

    @BeforeEach
    fun setUp() {
        val attributeFor = mockk<AttributeFor>()
        glucose = mockk<Attribute>()
        every { attributeFor(any()) } returns glucose
        conditionTip = ConditionTip(setOf("Glucose"), attributeFor)
    }

    @Test
    fun `should parse each type of expression to a condition`() {
        // Given
        val expressionToExpected = mapOf(
            "elevated glucose" to EpisodicCondition(null, glucose, High, Current, "elevated glucose"),
            "glucose is below the normal range" to EpisodicCondition(
                null,
                glucose,
                Low,
                Current,
                "glucose is below the normal range"
            ),
            "glucose is either low or normal" to EpisodicCondition(
                null,
                glucose,
                LowOrNormal,
                Current,
                "glucose is either low or normal"
            ),
            "glucose is either high or normal" to EpisodicCondition(
                null,
                glucose,
                HighOrNormal,
                Current,
                "glucose is either high or normal"
            ),
            "glucose is within the acceptable range" to EpisodicCondition(
                null,
                glucose,
                Normal,
                Current,
                "glucose is within the acceptable range"
            ),
            "glucose equals 3.14159" to EpisodicCondition(
                null,
                glucose,
                Is("3.14159"),
                Current,
                "glucose equals 3.14159"
            ),
            "glucose is pending" to EpisodicCondition(null, glucose, Is("\"pending\""), Current, "glucose is pending"),
            "glucose is cold" to EpisodicCondition(null, glucose, Is("\"cold\""), Current, "glucose is cold"),
            "glucose does not equal 3.14159" to EpisodicCondition(
                null,
                glucose,
                IsNot("3.14159"),
                Current,
                "glucose does not equal 3.14159"
            ),
            "glucose different to pending" to EpisodicCondition(
                null,
                glucose,
                IsNot("\"pending\""),
                Current,
                "glucose different to pending"
            ),
            "glucose isn't \"pending\"" to EpisodicCondition(
                null,
                glucose,
                IsNot("\"pending\""),
                Current,
                "glucose isn't \"pending\""
            ),
            "glucose is no more than 3.14159" to EpisodicCondition(
                null,
                glucose,
                LessThanOrEquals(3.14159),
                Current,
                "glucose is no more than 3.14159"
            ),
            "glucose smaller than 3.14159" to EpisodicCondition(
                null,
                glucose,
                LessThan(3.14159),
                Current,
                "glucose smaller than 3.14159"
            ),
            "glucose is at least 3.14159" to EpisodicCondition(
                null,
                glucose,
                GreaterThanOrEquals(3.14159),
                Current,
                "glucose is at least 3.14159"
            ),
            "glucose more than 3.14159" to EpisodicCondition(
                null,
                glucose,
                GreaterThan(3.14159),
                Current,
                "glucose more than 3.14159"
            ),
            "at most 3 glucose results are greater than or equal to 5.5" to EpisodicCondition(
                null, glucose, GreaterThanOrEquals(5.5),
                AtMost(3), "at most 3 glucose results are greater than or equal to 5.5"
            ),
            "glucose is a number" to EpisodicCondition(null, glucose, IsNumeric, Current, "glucose is a number"),
            "glucose is available" to CaseStructureCondition(null, IsPresentInCase(glucose), "glucose is available"),
            "glucose is not available" to CaseStructureCondition(
                null,
                IsAbsentFromCase(glucose),
                "glucose is not available"
            ),
            "case has only one episode" to CaseStructureCondition(
                null,
                IsSingleEpisodeCase,
                "case has only one episode"
            ),
            "every glucose result is normal" to EpisodicCondition(
                null,
                glucose,
                Normal,
                All,
                "every glucose result is normal"
            ),
            "every glucose result is abnormal" to EpisodicCondition(
                null,
                glucose,
                Normal,
                No,
                "every glucose result is abnormal"
            ),
            "every glucose result is elevated" to EpisodicCondition(
                null,
                glucose,
                High,
                All,
                "every glucose result is elevated"
            ),
            "no elevated glucose" to EpisodicCondition(null, glucose, High, No, "no elevated glucose"),
            "no more than 5 glucose results are elevated" to EpisodicCondition(
                null,
                glucose,
                High,
                AtMost(5),
                "no more than 5 glucose results are elevated"
            ),
            "every glucose result is low" to EpisodicCondition(null, glucose, Low, All, "every glucose result is low"),
            "no lowered glucose" to EpisodicCondition(null, glucose, Low, No, "no lowered glucose"),
            "at most 3 glucose results are below normal" to EpisodicCondition(
                null,
                glucose,
                Low,
                AtMost(3),
                "at most 3 glucose results are below normal"
            ),
            "every glucose result is a number" to EpisodicCondition(
                null,
                glucose,
                IsNumeric,
                All,
                "every glucose result is a number"
            ),
            "none of the glucose results are numeric" to EpisodicCondition(
                null,
                glucose,
                IsNumeric,
                No,
                "none of the glucose results are numeric"
            ),
            "glucose contains undefined" to EpisodicCondition(
                null,
                glucose,
                Contains("\"undefined\""),
                Current,
                "glucose contains undefined"
            ),
            "every glucose result contains undefined" to EpisodicCondition(
                null,
                glucose,
                Contains("\"undefined\""),
                All,
                "every glucose result contains undefined"
            ),
            "no glucose contains \"undefined\"" to EpisodicCondition(
                null,
                glucose,
                Contains("\"undefined\""),
                No,
                "no glucose contains \"undefined\""
            ),
            "none of the glucose results contain \"undefined\"" to EpisodicCondition(
                null,
                glucose,
                Contains("\"undefined\""),
                No,
                "none of the glucose results contain \"undefined\""
            ),
            "glucose does not contain undefined" to EpisodicCondition(
                null, glucose,
                DoesNotContain("\"undefined\""), Current, "glucose does not contain undefined"
            ),
            "glucose is lowered by no more than 15%" to EpisodicCondition(
                null, glucose,
                LowByAtMostSomePercentage(15), Current, "glucose is lowered by no more than 15%"
            ),
            "glucose is raised by no more than 15 percent%" to EpisodicCondition(
                null, glucose,
                HighByAtMostSomePercentage(15), Current, "glucose is raised by no more than 15 percent%"
            ),
            "glucose is either normal or not more than 15 percent below normal" to EpisodicCondition(
                null,
                glucose,
                NormalOrLowByAtMostSomePercentage(15),
                Current,
                "glucose is either normal or not more than 15 percent below normal"
            ),
            "glucose is either normal or not more than 15 percent above normal" to EpisodicCondition(
                null,
                glucose,
                NormalOrHighByAtMostSomePercentage(15),
                Current,
                "glucose is either normal or not more than 15 percent above normal"
            )
        )

        // When
        val results = conditionTip.conditionsFor(expressionToExpected.keys.toList())

        // Then
        results.forEachIndexed { index, actual ->
            val expression = expressionToExpected.keys.elementAt(index)
            val expected = expressionToExpected[expression]
            actual shouldBe expected
        }
    }

    @Test
    fun `should return null for an unparseable expression`() {
        // When
        val results = conditionTip.conditionsFor(listOf("gobbledygook"))

        // Then
        results[0] shouldBe null
    }

    @Test
    fun `should return null for expression with no attribute`() {
        // When
        val results = conditionTip.conditionsFor(listOf("high"))

        // Then
        results[0] shouldBe null
    }
}