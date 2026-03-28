package io.rippledown.server

import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

internal class ConditionExpressionParserTest {

    private val attributes = mutableMapOf<String, Attribute>()
    private var nextId = 1
    private val attributeFor: (String) -> Attribute = { name ->
        attributes.getOrPut(name) { Attribute(nextId++, name) }
    }
    private val parser = ConditionExpressionParser(attributeFor)

    @Test
    fun `should parse case is for a single date`() {
        val condition = parser.parse("case is for a single date")
        assertIs<CaseStructureCondition>(condition)
        assertEquals(IsSingleEpisodeCase, condition.predicate)
    }

    @Test
    fun `should parse at least N are numeric`() {
        val condition = parser.parse("at least 2 TSH are numeric")
        assertIs<EpisodicCondition>(condition)
        assertEquals("TSH", condition.attribute.name)
        assertEquals(IsNumeric, condition.predicate)
        assertEquals(AtLeast(2), condition.signature)
    }

    @Test
    fun `should parse attribute increasing`() {
        val condition = parser.parse("Free T3 increasing")
        assertIs<SeriesCondition>(condition)
        assertEquals("Free T3", condition.attribute.name)
        assertEquals(Increasing, condition.seriesPredicate)
    }

    @Test
    fun `should parse attribute decreasing`() {
        val condition = parser.parse("Free T3 decreasing")
        assertIs<SeriesCondition>(condition)
        assertEquals("Free T3", condition.attribute.name)
        assertEquals(Decreasing, condition.seriesPredicate)
    }

    @Test
    fun `should parse attribute is in case`() {
        val condition = parser.parse("Thyroglobulin is in case")
        assertIs<CaseStructureCondition>(condition)
        val predicate = condition.predicate
        assertIs<IsPresentInCase>(predicate)
        assertEquals("Thyroglobulin", predicate.attribute.name)
    }

    @Test
    fun `should parse attribute is not in case`() {
        val condition = parser.parse("Free T3 is not in case")
        assertIs<CaseStructureCondition>(condition)
        val predicate = condition.predicate
        assertIs<IsAbsentFromCase>(predicate)
        assertEquals("Free T3", predicate.attribute.name)
    }

    @Test
    fun `should parse does not contain`() {
        val condition = parser.parse("""Clinical Notes does not contain "/40"""")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Clinical Notes", condition.attribute.name)
        assertEquals(DoesNotContain("/40"), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse contains`() {
        val condition = parser.parse("""Clinical Notes contains "On T4 replacement"""")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Clinical Notes", condition.attribute.name)
        assertEquals(Contains("On T4 replacement"), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is normal or high by at most N percent`() {
        val condition = parser.parse("Free T4 is normal or high by at most 10%")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Free T4", condition.attribute.name)
        assertEquals(NormalOrHighByAtMostSomePercentage(10), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is normal or low by at most N percent`() {
        val condition = parser.parse("Free T4 is normal or low by at most 20%")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Free T4", condition.attribute.name)
        assertEquals(NormalOrLowByAtMostSomePercentage(20), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is low by at most N percent`() {
        val condition = parser.parse("Free T4 is low by at most 20%")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Free T4", condition.attribute.name)
        assertEquals(LowByAtMostSomePercentage(20), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is high by at most N percent`() {
        val condition = parser.parse("Free T4 is high by at most 15%")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Free T4", condition.attribute.name)
        assertEquals(HighByAtMostSomePercentage(15), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is normal`() {
        val condition = parser.parse("TSH is normal")
        assertIs<EpisodicCondition>(condition)
        assertEquals("TSH", condition.attribute.name)
        assertEquals(Normal, condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is high`() {
        val condition = parser.parse("TSH is high")
        assertIs<EpisodicCondition>(condition)
        assertEquals("TSH", condition.attribute.name)
        assertEquals(High, condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is low`() {
        val condition = parser.parse("TSH is low")
        assertIs<EpisodicCondition>(condition)
        assertEquals("TSH", condition.attribute.name)
        assertEquals(Low, condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is quoted value`() {
        val condition = parser.parse("""Sex is "female"""")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Sex", condition.attribute.name)
        assertEquals(Is("female"), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse is quoted value with angle brackets`() {
        val condition = parser.parse("""TSH is "<0.01"""")
        assertIs<EpisodicCondition>(condition)
        assertEquals("TSH", condition.attribute.name)
        assertEquals(Is("<0.01"), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse greater than or equals`() {
        val condition = parser.parse("Age ≥ 70.0")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Age", condition.attribute.name)
        assertEquals(GreaterThanOrEquals(70.0), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse less than or equals`() {
        val condition = parser.parse("TSH ≤ 0.1")
        assertIs<EpisodicCondition>(condition)
        assertEquals("TSH", condition.attribute.name)
        assertEquals(LessThanOrEquals(0.1), condition.predicate)
        assertEquals(Current, condition.signature)
    }

    @Test
    fun `should parse multi-word attribute with is normal`() {
        val condition = parser.parse("Free T4 is normal")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Free T4", condition.attribute.name)
        assertEquals(Normal, condition.predicate)
    }

    @Test
    fun `should parse multi-word attribute with contains`() {
        val condition = parser.parse("""Clinical Notes contains "12/40 weeks"""")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Clinical Notes", condition.attribute.name)
        assertEquals(Contains("12/40 weeks"), condition.predicate)
    }

    @Test
    fun `should parse attribute with hyphen`() {
        val condition = parser.parse("Anti-Thyroglobulin is high")
        assertIs<EpisodicCondition>(condition)
        assertEquals("Anti-Thyroglobulin", condition.attribute.name)
        assertEquals(High, condition.predicate)
    }

    @Test
    fun `should preserve user expression`() {
        val expression = "TSH is normal"
        val condition = parser.parse(expression)
        assertEquals(expression, condition.userExpression())
    }

    @Test
    fun `should throw for unrecognised expression`() {
        assertFailsWith<IllegalArgumentException> {
            parser.parse("something completely unknown")
        }
    }
}
