package io.rippledown.conditiongenerator

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.expressionparser.AttributeFor
import io.rippledown.model.Attribute
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SignatureGenerationTest {
    private lateinit var attributeFor: AttributeFor
    private lateinit var attribute: Attribute
    private lateinit var generator: ConditionGenerator

    @BeforeEach
    fun setUp() {
        attributeFor = mockk<AttributeFor>()
        attribute = mockk<Attribute>()
        generator = ConditionGenerator(attributeFor)
    }

    @Test
    fun `should generate a signature no constructor`() {
        val spec = FunctionSpecification(Current::class.simpleName!!, listOf())
        generator.signatureFrom(spec) shouldBe Current
    }

    @Test
    fun `should generate a signature with Int parameter`() {
        val spec = FunctionSpecification(AtLeast::class.simpleName!!, listOf("42"))
        generator.signatureFrom(spec) shouldBe AtLeast(42)
    }
}