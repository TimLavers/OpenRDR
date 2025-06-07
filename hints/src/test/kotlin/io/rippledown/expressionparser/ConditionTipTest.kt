package io.rippledown.expressionparser

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
    private lateinit var TSH: Attribute
    private lateinit var XYZ: Attribute
    private lateinit var waves: Attribute

    @BeforeEach
    fun setUp() {
        val attributeFor = mockk<AttributeFor>()
        glucose = mockk<Attribute>()
        every { attributeFor("Glucose") } returns glucose
        conditionTip = ConditionTip(setOf("Glucose"), attributeFor)
    }

    @Test
    fun `should return null if no condition can be parsed`() {
        conditionTip.conditionFor("gobbledygook") shouldBe null
    }

    @Test
    fun `should return null for a valid predicate but when no attribute is defined`() {
        val tip = ConditionTip(emptySet(), mockk())
        tip.conditionFor("high") shouldBe null
    }

    @Test
    fun `should parse expression to High`() {
        // Given
        val expression = "elevated glucose"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, High, Current, expression)
    }

    @Test
    fun `should parse expression to Low`() {
        // Given
        val expression = "glucose is below the normal range"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Low, Current, expression)
    }

    @Test
    fun `should parse expression to LowOrNormal`() {
        // Given
        val expression = "glucose is either low or normal"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, LowOrNormal, Current, expression)
    }

    @Test
    fun `should parse expression to HighOrNormal`() {
        // Given
        val expression = "glucose is either high or normal"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, HighOrNormal, Current, expression)
    }

    @Test
    fun `should parse expression to Normal`() {
        // Given
        val expression = "glucose is within the acceptable range"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Normal, Current, expression)
    }

    @Test
    fun `should parse expression to Is`() {
        // Given
        val expression = "glucose equals 3.14159"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Is("3.14159"), Current, expression)
    }

    @Test
    fun `should parse expression to Is with pending quoted`() {
        // Given
        val expression = "glucose is pending"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Is("\"pending\""), Current, expression)
    }

    @Test
    fun `should parse expression to IsNot`() {
        // Given
        val expression = "glucose does not equal 3.14159"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, IsNot("3.14159"), Current, expression)
    }

    @Test
    fun `should parse expression to IsNot with value unquoted`() {
        // Given
        val expression = "glucose different to pending"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, IsNot("\"pending\""), Current, expression)
    }

    @Test
    fun `should parse expression to IsNot with value quoted`() {
        // Given
        val expression = "glucose isn't \"pending\""

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, IsNot("\"pending\""), Current, expression)
    }

    @Test
    fun `should parse expression to LessThanOrEquals`() {
        // Given
        val expression = "glucose is no more than 3.14159"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, LessThanOrEquals(3.14159), Current, expression)
    }

    @Test
    fun `should parse expression to GreaterThanOrEquals`() {
        // Given
        val expression = "glucose is at least 3.14159"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, GreaterThanOrEquals(3.14159), Current, expression)
    }

    @Test
    fun `should parse expression to GreaterThanOrEquals with signature AtMost`() {
        // Given
        val expression = "at most 3 glucose results are greater than or equal to 5.5"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, GreaterThanOrEquals(5.5), AtMost(3), expression)
    }

    @Test
    fun `should parse expression to IsNumeric`() {
        // Given
        val expression = "glucose is a number"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, IsNumeric, Current, expression)
    }

    @Test
    fun `should parse expression to IsPresentInCase`() {
        // Given
        val expression = "glucose is available"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe CaseStructureCondition(null, IsPresentInCase(glucose), expression)
    }

    @Test
    fun `should parse expression to IsAbsentFromCase`() {
        // Given
        val expression = "glucose is not available"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe CaseStructureCondition(null, IsAbsentFromCase(glucose), expression)
    }

    @Test
    fun `should parse expression to SingleEpisodeCase`() {
        // Given
        val expression = "case has only one episode"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe CaseStructureCondition(null, IsSingleEpisodeCase, expression)
    }

    @Test
    fun `should parse expression to Normal with signature All`() {
        // Given
        val expression = "every glucose result is normal"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Normal, All, expression)
    }

    @Test
    fun `should parse expression to Normal with signature No`() {
        // Given
        val expression = "every glucose result is abnormal"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Normal, No, expression)
    }

    @Test
    fun `should parse expression to High with signature All`() {
        // Given
        val expression = "every glucose result is elevated"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, High, All, expression)
    }

    @Test
    fun `should parse expression to High with signature No`() {
        // Given
        val expression = "no elevated glucose"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, High, No, expression)
    }

    @Test
    fun `should parse expression to High with signature AtMost`() {
        // Given
        val expression = "no more than 5 glucose results are elevated"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, High, AtMost(5), expression)
    }

    @Test
    fun `should parse expression to Low with signature All`() {
        // Given
        val expression = "every glucose result is low"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Low, All, expression)
    }

    @Test
    fun `should parse expression to Low with signature No`() {
        // Given
        val expression = "no lowered glucose"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Low, No, expression)
    }

    @Test
    fun `should parse expression to Low with signature AtMost`() {
        // Given
        val expression = "at most 3 glucose results are below normal"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Low, AtMost(3), expression)
    }

    @Test
    fun `should parse expression to IsNumeric with signature All`() {
        // Given
        val expression = "every glucose result is a number"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, IsNumeric, All, expression)
    }

    @Test
    fun `should parse expression to IsNumeric with signature No`() {
        // Given
        val expression = "none of the glucose results are numeric"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, IsNumeric, No, expression)
    }

    @Test
    fun `should parse expression to Contains`() {
        // Given
        val expression = "glucose contains undefined"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Contains("\"undefined\""), Current, expression)
    }

    @Test
    fun `should parse expression to Contains with signature All`() {
        // Given
        val expression = "every glucose result contains undefined"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Contains("\"undefined\""), All, expression)
    }

    @Test
    fun `should parse expression to Contains with signature No`() {
        // Given
        val expression = "no glucose contains \"undefined\""

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Contains("\"undefined\""), No, expression)
    }

    @Test
    fun `should parse expression to Contains with No signature variant`() {
        // Given
        val expression = "none of the glucose results contain \"undefined\""

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, Contains("\"undefined\""), No, expression)
    }

    @Test
    fun `should parse expression to DoesNotContain`() {
        // Given
        val expression = "glucose does not contain undefined"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, DoesNotContain("\"undefined\""), Current, expression)
    }

    @Test
    fun `should parse expression to LowByAtMostSomePercentage`() {
        // Given
        val expression = "glucose is lowered by no more than 15%"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, LowByAtMostSomePercentage(15), Current, expression)
    }

    @Test
    fun `should parse expression to HighByAtMostSomePercentage`() {
        // Given
        val expression = "glucose is raised by no more than 15 percent%"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, HighByAtMostSomePercentage(15), Current, expression)
    }

    @Test
    fun `should parse expression to NormalOrLowByAtMostSomePercentage`() {
        // Given
        val expression = "glucose is either normal or not more than 15 percent below normal"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, NormalOrLowByAtMostSomePercentage(15), Current, expression)
    }

    @Test
    fun `should parse expression to NormalOrHighByAtMostSomePercentage`() {
        // Given
        val expression = "glucose is either normal or not more than 15 percent above normal"

        // When
        val actual = conditionTip.conditionFor(expression)

        // Then
        actual shouldBe EpisodicCondition(null, glucose, NormalOrHighByAtMostSomePercentage(15), Current, expression)
    }
}
