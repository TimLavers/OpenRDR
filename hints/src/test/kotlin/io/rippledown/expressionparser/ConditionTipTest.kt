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
    fun `should retrieve a single syntactically valid condition`() {
        // Given
        val expression = "elevated glucose"

        // When
        val actual = conditionTipGenerator.conditionFor(expression)

        // Then
        actual shouldBe constructors.High(glucose, expression)
        actual!!.userExpression() shouldBe expression
    }

    @Test
    fun `should suggest syntactically valid conditions`() {
        // Given
        with(constructors) {
            val elevatedWaves = "elevated waves"
            val low = "tsh is below the normal range"
            val isConstant = "xyz = 3.14159"
            val equalsConstant = "xyz equals 3.14159"
            val lte = "xyz is no more than 3.14159"
            val gte = "xyz is at least 3.14159"
            val numeric = "xyz is a number"
            val present = "xyz is available"
            val isPending = "glucose is pending"
            val tall = "very tall waves"
            val slightlyLow = "glucose no more than 15 percent below normal"
            val normalOrSlightlyLow = "glucose is either normal or not more than 15 percent below normal"
            val slightlyHigh = "glucose no more than 15 percent above normal"
            val normalOrSlightlyHigh = "glucose is either normal or high by no more than 15 percent"

            val expectations = listOf(
                low to Low(TSH, low),
                isConstant to Is(XYZ, isConstant, "3.14159"),
                equalsConstant to Is(XYZ, equalsConstant, "3.14159"),
                lte to LessThanOrEqualTo(XYZ, lte, "3.14159"),
                gte to GreaterThanOrEqualTo(XYZ, gte, "3.14159"),
                numeric to Numeric(XYZ, numeric),
                present to Present(XYZ, present),
                isPending to Is(glucose, isPending, "\"pending\""),
                elevatedWaves to High(Waves, elevatedWaves),
                tall to High(Waves, tall),
                slightlyLow to SlightlyLow(glucose, slightlyLow, "15"),
                normalOrSlightlyLow to NormalOrSlightlyLow(glucose, normalOrSlightlyLow, "15"),
                slightlyHigh to SlightlyHigh(glucose, slightlyHigh, "15"),
                normalOrSlightlyHigh to NormalOrSlightlyHigh(glucose, normalOrSlightlyHigh, "15"),
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