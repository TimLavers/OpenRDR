package io.rippledown.model.chat

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class KnowledgeBaseListingTest {

    @Test
    fun `descriptions default to empty and round-trip through JSON`() {
        // Given
        val listing = KnowledgeBaseListing(
            listOf("Thyroids"), listOf("Zoo Animals"), "Thyroids",
            mapOf("Thyroids" to "Thyroid rules.", "Zoo Animals" to "Animals.")
        )

        // When / Then
        KnowledgeBaseListing(listOf("A"), emptyList()).descriptions shouldBe emptyMap()
        serializeDeserialize(listing) shouldBe listing
    }

    @Test
    fun `summary is the first non-blank line with any heading marks removed`() {
        // Given
        val description = "\n  # Thyroids  \nA basic thyroid management KB.\nSee: https://thyroid.rules.info/basic"

        // When
        val summary = summaryOf(description)

        // Then
        summary shouldBe "Thyroids"
    }

    @Test
    fun `summary of a blank description is blank`() {
        // Given / When / Then
        summaryOf("") shouldBe ""
        summaryOf(" \n\n ") shouldBe ""
    }

    @Test
    fun `summary keeps a short single line unchanged`() {
        // Given / When / Then
        summaryOf("Interpretative comments for thyroid function tests.") shouldBe
                "Interpretative comments for thyroid function tests."
    }

    @Test
    fun `summary is truncated at the maximum length with an ellipsis`() {
        // Given
        val long = "x".repeat(SUMMARY_MAX_LENGTH + 10)

        // When
        val summary = summaryOf(long)

        // Then
        summary.length shouldBe SUMMARY_MAX_LENGTH
        summary shouldBe "x".repeat(SUMMARY_MAX_LENGTH - 1) + "…"
    }

    @Test
    fun `summary of exactly the maximum length is not truncated`() {
        // Given
        val exact = "y".repeat(SUMMARY_MAX_LENGTH)

        // When / Then
        summaryOf(exact) shouldBe exact
    }
}
