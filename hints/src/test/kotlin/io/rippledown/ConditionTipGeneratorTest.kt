package io.rippledown

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionTipGeneratorTest {

    private lateinit var conditionTipGenerator: ConditionTipGenerator

    @BeforeEach
    fun setUp() {
        conditionTipGenerator = ConditionTipGenerator(setOf("Glucose", "TSH", "XYZ", "Waves"))
    }

    @Test
    fun `should suggest syntactically valid conditions`() {
        conditionTipGenerator.conditionTip("elevated glucose") shouldBe "Glucose is high"
        conditionTipGenerator.conditionTip("tsh is below the normal range") shouldBe "TSH is low"
        conditionTipGenerator.conditionTip("xyz = 3.14159") shouldBe "XYZ is 3.14159"
        conditionTipGenerator.conditionTip("glucose is pending") shouldBe "Glucose is \"pending\""
        conditionTipGenerator.conditionTip("elevated waves") shouldBe "Waves is high"
        conditionTipGenerator.conditionTip("very tall waves") shouldBe "Waves is high"
    }
}