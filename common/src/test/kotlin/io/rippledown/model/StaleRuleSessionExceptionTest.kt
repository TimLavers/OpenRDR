package io.rippledown.model

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StaleRuleSessionExceptionTest {

    @Test
    fun `interpretation changed message names the case`() {
        // When
        val message = interpretationChangedMessage("Case1")

        // Then
        message shouldBe "The interpretation of Case1 changed while you were building this rule. " +
                "The rule session has been cancelled; please look at the case again."
    }

    @Test
    fun `cornerstones changed message is fixed`() {
        // When
        val message = cornerstonesChangedMessage()

        // Then
        message shouldBe "The cornerstones changed while you were building this rule. " +
                "The rule session has been cancelled; please look at the case again."
    }

    @Test
    fun `exception carries its message`() {
        // When
        val exception = StaleRuleSessionException(interpretationChangedMessage("Case1"))

        // Then
        exception.message shouldBe interpretationChangedMessage("Case1")
    }
}
