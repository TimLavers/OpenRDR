package io.rippledown.expressionparser

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionTipTest {

    private lateinit var conditionTip: ConditionTip
    private lateinit var glucose: Attribute
    private lateinit var TSH: Attribute
    private lateinit var XYZ: Attribute
    private lateinit var waves: Attribute

    @BeforeEach
    fun setUp() {
        val attributeFor = mockk<AttributeFor>()

        glucose = mockk<Attribute>()
        TSH = mockk<Attribute>()
        XYZ = mockk<Attribute>()
        waves = mockk<Attribute>()
        every { attributeFor("Glucose") } returns glucose
        every { attributeFor("TSH") } returns TSH
        every { attributeFor("XYZ") } returns XYZ
        every { attributeFor("Waves") } returns waves

        conditionTip = ConditionTip(setOf("Glucose", "TSH", "XYZ", "Waves"), attributeFor)
    }

    @Test
    fun `should return null if no condition can be parsed`() {
        conditionTip.conditionFor("gobbledygook") shouldBe null
    }

    @Test
    fun `should retrieve a single syntactically valid condition`() {
        // Given
        val expression = "elevated glucose"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, High, Current, expression)
    }

    @Test
    fun `should suggest syntactically valid conditions`() {
        // Given
        val highExpression = "elevated waves"
        val lowExpression = "glucose is below the normal range"
        val normalExpression = "glucose is within the acceptable range"
        val isExpression = "xyz equals 3.14159"
        val lteExpression = "xyz is no more than 3.14159"
        val gteExpression = "xyz is at least 3.14159"
        val numericExpression = "xyz is a number"
        val presentExpression = "xyz is available"
        val absentExpression = "xyz is not available"
        //todo
        val isPending = "glucose is pending"
        val slightlyLow = "glucose no more than 15 percent below normal"
        val normalOrSlightlyLow = "glucose is either normal or not more than 15 percent below normal"
        val slightlyHigh = "glucose no more than 15 percent above normal"
        val normalOrSlightlyHigh = "glucose is either normal or high by no more than 15 percent"
        val singleEpisodeCase = "case has only one episode"
        val allNormal = "every glucose result is normal"
        val noNormal = "every glucose result is abnormal"
        val allHigh = "every glucose result is high"
        val noHigh = "no elevated glucose"
        val allLow = "every glucose result is low"
        val noLow = "no lowered glucose"
        val allContain = "every xyz result contains undefined"
        val noContain = "none of the xyz results contain \"undefined\""
        val allNumeric = "every xyz result is a number"
        val noNumeric = "none of the xyz results are numeric"
        val atMostHigh = "at most 5 glucose results are high"
        val atMostLow = "at most 3 glucose results are below normal"
        val atMostGreaterThanOrEqualTo = "at most 3 glucose results are greater than or equal to 5.5"

        val expectations = listOf(
            highExpression to EpisodicCondition(null, waves, High, Current, highExpression),
            lowExpression to EpisodicCondition(null, glucose, Low, Current, lowExpression),
            normalExpression to EpisodicCondition(null, glucose, Normal, Current, normalExpression),
            isExpression to EpisodicCondition(null, XYZ, Is("3.14159"), Current, isExpression),
            lteExpression to EpisodicCondition(null, XYZ, LessThanOrEquals(3.14159), Current, lteExpression),
            gteExpression to EpisodicCondition(null, XYZ, GreaterThanOrEquals(3.14159), Current, gteExpression),
            numericExpression to EpisodicCondition(null, XYZ, IsNumeric, Current, numericExpression),
            presentExpression to CaseStructureCondition(null, IsPresentInCase(XYZ), presentExpression),
            absentExpression to CaseStructureCondition(null, IsAbsentFromCase(XYZ), absentExpression),
            /*
             todo
             isPending to Is(glucose, isPending, "\"pending\""),
             slightlyLow to SlightlyLow(glucose, slightlyLow, "15"),
             normalOrSlightlyLow to NormalOrSlightlyLow(glucose, normalOrSlightlyLow, "15"),
             slightlyHigh to SlightlyHigh(glucose, slightlyHigh, "15"),
             normalOrSlightlyHigh to NormalOrSlightlyHigh(glucose, normalOrSlightlyHigh, "15"),
             singleEpisodeCase to SingleEpisodeCase(singleEpisodeCase),
             allNormal to AllNormal(glucose, allNormal),
             noNormal to NoNormal(glucose, noNormal),
             allHigh to AllHigh(glucose, allHigh),
             noHigh to NoHigh(glucose, noHigh),
             allLow to AllLow(glucose, allLow),
             noLow to NoLow(glucose, noLow),
             allContain to AllContain(XYZ, allContain, "\"undefined\""),
             noContain to NoContain(XYZ, noContain, "\"undefined\""),
             allNumeric to AllNumeric(XYZ, allNumeric),
             noNumeric to NoNumeric(XYZ, noNumeric),
             atMostHigh to AtMostHigh(glucose, atMostHigh, "5"),
             atMostLow to AtMostLow(glucose, atMostLow, "3"),
             atMostGreaterThanOrEqualTo to AtMostGreaterThanOrEqualTo(
                 glucose,
                 atMostGreaterThanOrEqualTo,
                 "3",
                 "5.5"
             ),*/
        )
        var errors = 0
        expectations.forEach { (input, expected) ->
            try {
                // When
                val actual = conditionTip.conditionFor(input)

                // Then
                actual shouldBe expected
            } catch (e: Error) {
                errors++
                println("Failed for $input")
                e.printStackTrace()
            }
        }
        errors shouldBe 0
    }
}
