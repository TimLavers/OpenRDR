package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.utils.serializeDeserialize
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ConditionParsingResultTest {
    @Test
    fun `round trips the incoming expression separately from the stored phrase`() {
        // Given
        val condition = isHigh(7, Attribute(1, "Glucose"), "elevated glucose")
        val result = ConditionParsingResult(condition, expression = "raised glucose")

        // When
        val restored = serializeDeserialize(result)

        // Then
        restored shouldBe result
        restored.expression shouldBe "raised glucose"
        restored.condition?.userExpression() shouldBe "elevated glucose"
        restored.isFailure shouldBe false
    }

    @Test
    fun `decodes a legacy result without an incoming expression`() {
        // Given
        val json = """{"condition":null,"errorMessage":"Not recognised"}"""

        // When
        val restored = Json.decodeFromString<ConditionParsingResult>(json)

        // Then
        restored.expression shouldBe ""
        restored.errorMessage shouldBe "Not recognised"
        restored.isFailure shouldBe true
    }

    @Test
    fun `round trips a failure with the incoming expression`() {
        // Given
        val result = ConditionParsingResult(errorMessage = "Not recognised", expression = "something else")

        // When
        val restored = serializeDeserialize(result)

        // Then
        restored shouldBe result
        restored.isFailure shouldBe true
    }
}
