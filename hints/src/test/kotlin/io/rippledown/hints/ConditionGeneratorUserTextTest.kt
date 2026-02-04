package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionGeneratorUserTextTest {
    private lateinit var attributeFor: AttributeFor
    private lateinit var attribute: Attribute
    private lateinit var chatService: ConditionChatService
    private lateinit var generator: ConditionGenerator
    private val attributeName = "glucose"

    @BeforeEach
    fun setUp() {
        attributeFor = mockk<AttributeFor>()
        attribute = mockk<Attribute>()
        chatService = mockk<ConditionChatService>()
        every { attributeFor(attributeName) } returns attribute
        generator = ConditionGenerator(attributeFor, chatService)
    }

    @Test
    fun `should return null for empty string`() {
        //When
        val condition = generator.conditionFor("")

        //Then
        condition shouldBe null
    }

    @Test
    fun `should return null for whitespace-only string`() {
        //When
        val condition = generator.conditionFor("   ")

        //Then
        condition shouldBe null
    }

    @Test
    fun `should return null when chat service returns null`() {
        //Given
        coEvery { chatService.transform(any()) } returns null

        //When
        val condition = generator.conditionFor("glucose is high")

        //Then
        condition shouldBe null
    }

    @Test
    fun `should return episodic condition when chat service returns valid spec`() {
        //Given
        val userText = "glucose is elevated"
        val spec = ConditionSpecification(
            userText,
            attributeName,
            FunctionSpecification(High::class.simpleName!!, listOf()),
            FunctionSpecification(Current::class.simpleName!!, listOf())
        )
        coEvery { chatService.transform(userText) } returns spec

        //When
        val condition = generator.conditionFor(userText)

        //Then
        condition shouldNotBe null
        condition shouldBe EpisodicCondition(null, attribute, High, Current, userText)
    }

    @Test
    fun `should return case structure condition when chat service returns valid spec`() {
        //Given
        val userText = "case has only one episode"
        val spec = ConditionSpecification(
            userText,
            null,
            FunctionSpecification(IsSingleEpisodeCase::class.simpleName!!, listOf())
        )
        coEvery { chatService.transform(userText) } returns spec

        //When
        val condition = generator.conditionFor(userText)

        //Then
        condition shouldNotBe null
        condition shouldBe CaseStructureCondition(null, IsSingleEpisodeCase, userText)
    }

    @Test
    fun `should return series condition when chat service returns valid spec`() {
        //Given
        val userText = "glucose is increasing"
        val spec = ConditionSpecification(
            userText,
            attributeName,
            FunctionSpecification(Increasing::class.simpleName!!, listOf())
        )
        coEvery { chatService.transform(userText) } returns spec

        //When
        val condition = generator.conditionFor(userText)

        //Then
        condition shouldNotBe null
        condition shouldBe SeriesCondition(null, attribute, Increasing, userText)
    }

    @Test
    fun `should return null when chat service throws exception`() {
        //Given
        coEvery { chatService.transform(any()) } throws RuntimeException("API error")

        //When
        val condition = generator.conditionFor("glucose is high")

        //Then
        condition shouldBe null
    }

    @Test
    fun `should return condition with predicate parameters`() {
        //Given
        val userText = "glucose contains undefined"
        val spec = ConditionSpecification(
            userText,
            attributeName,
            FunctionSpecification(Contains::class.simpleName!!, listOf("undefined")),
            FunctionSpecification(Current::class.simpleName!!, listOf())
        )
        coEvery { chatService.transform(userText) } returns spec

        //When
        val condition = generator.conditionFor(userText)

        //Then
        condition shouldBe EpisodicCondition(null, attribute, Contains("undefined"), Current, userText)
    }

    @Test
    fun `should return condition with signature parameters`() {
        //Given
        val userText = "at least 3 glucose values are low"
        val spec = ConditionSpecification(
            userText,
            attributeName,
            FunctionSpecification(Low::class.simpleName!!, listOf()),
            FunctionSpecification(AtLeast::class.simpleName!!, listOf("3"))
        )
        coEvery { chatService.transform(userText) } returns spec

        //When
        val condition = generator.conditionFor(userText)

        //Then
        condition shouldBe EpisodicCondition(null, attribute, Low, AtLeast(3), userText)
    }
}
