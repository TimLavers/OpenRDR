package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.utils.serializeDeserialize
import org.junit.jupiter.api.Test

class ConditionTextTest : ConditionTestBase() {

    @Test
    fun `has a phrase when it differs from the formal text`() {
        // Given
        val text = ConditionText("TSH is high", "elevated TSH")

        // When
        val hasPhrase = text.hasPhrase()

        // Then
        hasPhrase shouldBe true
    }

    @Test
    fun `has no phrase when the phrase is blank`() {
        // Given
        val text = ConditionText("TSH is high", " ")

        // When
        val hasPhrase = text.hasPhrase()

        // Then
        hasPhrase shouldBe false
    }

    @Test
    fun `has no phrase when the phrase equals the formal text`() {
        // Given
        val text = ConditionText("TSH is high", "TSH is high")

        // When
        val hasPhrase = text.hasPhrase()

        // Then
        hasPhrase shouldBe false
    }

    @Test
    fun `phrase defaults to blank`() {
        // Given
        val formal = "TSH is high"

        // When
        val text = ConditionText(formal)

        // Then
        text.phrase shouldBe ""
        text.hasPhrase() shouldBe false
    }

    @Test
    fun `is built from a condition's formal text and user expression`() {
        // Given
        val condition = EpisodicCondition(7, tsh, High, Current, "elevated TSH")

        // When
        val text = ConditionText.of(condition)

        // Then
        text shouldBe ConditionText("TSH is high", "elevated TSH")
    }

    @Test
    fun serialization() {
        // Given
        val text = ConditionText("TSH is high", "elevated TSH")

        // When
        val restored = serializeDeserialize(text)

        // Then
        restored shouldBe text
    }
}
