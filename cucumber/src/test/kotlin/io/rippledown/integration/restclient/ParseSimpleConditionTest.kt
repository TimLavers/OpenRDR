package io.rippledown.integration.restclient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.predicate.IsNotBlank
import io.rippledown.model.condition.episodic.signature.Current
import kotlin.test.BeforeTest
import kotlin.test.Test

class ParseSimpleConditionTest {
    private lateinit var attributeGetter: AttributeGetter
    private lateinit var attribute: Attribute

    @BeforeTest
    fun setup() {
        attributeGetter = mockk()
        attribute = mockk()
        every { attributeGetter("x") } returns attribute
    }

    @Test
    fun `should parse 'x is in case' to a condition`() {
        //Given
        val expression = "x is in case"
        every { attributeGetter("x") } returns attribute

        //When
        val condition = parseToCondition(expression, attributeGetter)

        //Then
        val expected = EpisodicCondition(null, attribute, IsNotBlank, Current)
        condition shouldBe expected
    }

    @Test
    fun `should parse 'x is numeric value' to a condition`() {
        //Given
        val expression = "x is 42.1"

        //When
        val condition = parseToCondition(expression, attributeGetter)

        //Then
        val expected = EpisodicCondition(null, attribute, Is("42.1"), Current)
        condition shouldBe expected
    }

    @Test
    fun `should parse 'x is text value' to a condition`() {
        //Given
        val expression = "x is abc"

        //When
        val condition = parseToCondition(expression, attributeGetter)

        //Then
        val expected = EpisodicCondition(null, attribute, Is("abc"), Current)
        condition shouldBe expected
    }

    @Test
    fun `should parse 'x contains text' to a condition`() {
        //Given
        val expression = "x contains abc"

        //When
        val condition = parseToCondition(expression, attributeGetter)

        //Then
        val expected = EpisodicCondition(null, attribute, Contains("abc"), Current)
        condition shouldBe expected
    }

    @Test
    fun `should parse 'x contains numeric' to a condition`() {
        //Given
        val expression = "x contains 123"

        //When
        val condition = parseToCondition(expression, attributeGetter)

        //Then
        val expected = EpisodicCondition(null, attribute, Contains("123"), Current)
        condition shouldBe expected
    }

    @Test
    fun `should throw exception for unsupported expression`() {
        //Given
        val expression = "x equals 5"

        //When/Then
        val exception = shouldThrow<IllegalArgumentException> {
            parseToCondition(expression, attributeGetter)
        }
        exception.message shouldBe "Only 'is in case', 'is <value>', or 'contains <value>' is supported"
    }
}
