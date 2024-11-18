package io.rippledown.expressionparser

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionConstructors
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionTipTest {

    private lateinit var conditionTipGenerator: ConditionTip
    private val constructors = ConditionConstructors()
    private lateinit var glucose: Attribute
    private lateinit var TSH: Attribute
    private lateinit var XYZ: Attribute
    private lateinit var Waves: Attribute

    @BeforeEach
    fun setUp() {
        val attributeFor = mockk<AttributeFor>()

        glucose = mockk<Attribute>()
        TSH = mockk<Attribute>()
        XYZ = mockk<Attribute>()
        Waves = mockk<Attribute>()
        every { attributeFor("Glucose") } returns glucose
        every { attributeFor("TSH") } returns TSH
        every { attributeFor("XYZ") } returns XYZ
        every { attributeFor("Waves") } returns Waves

        conditionTipGenerator = ConditionTip(setOf("Glucose", "TSH", "XYZ", "Waves"), attributeFor)
    }

    @Test
    fun `should return null if no condition can be parsed`() {
        conditionTipGenerator.conditionFor("gobbledygook") shouldBe null
    }

    @Test
    fun `should suggest syntactically valid conditions`() {
        // Given
        with(constructors) {
            val expectations = listOf(
                "elevated glucose" to High(glucose, "elevated glucose"),
                "tsh is below the normal range" to Low(TSH, "tsh is below the normal range"),
                "xyz = 3.14159" to Is(XYZ, "xyz = 3.14159", "3.14159"),
                "xyz equals 3.14159" to Is(XYZ, "xyz equals 3.14159", "3.14159"),
                "xyz is no more than 3.14159" to LessThanOrEqualTo(XYZ, "xyz is no more than 3.14159", "3.14159"),
                "xyz is at least 3.14159" to GreaterThanOrEqualTo(XYZ, "xyz is at least 3.14159", "3.14159"),
                "xyz is a number" to Numeric(XYZ, "xyz is a number"),
                "xyz is available" to Present(XYZ, "xyz is available"),
                "glucose is pending" to Is(glucose, "glucose is pending", "\"pending\""),
                "elevated waves" to High(Waves, "elevated waves"),
                "very tall waves" to High(Waves, "very tall waves"),
            )

            expectations.forEach { (input, expected) ->
                withClue("Entered '$input'") {
                    // When
                    val actual = conditionTipGenerator.conditionFor(input)

                    // Then
                    actual shouldBe expected
                    actual!!.userExpression() shouldBe input
                }
            }
        }
    }
}