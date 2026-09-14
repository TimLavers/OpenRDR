package io.rippledown.sample

import io.kotest.matchers.shouldBe
import io.rippledown.sample.SampleKB.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class SampleKBTest {

    @Test
    fun values() {
        // Given
        val expected = listOf(TSH, TSH_CASES, CONTACT_LENSES, CONTACT_LENSES_CASES, ZOO, ZOO_CASES, PATHOLOGY)

        // When
        val all = SampleKB.entries.toTypedArray()

        // Then
        all.size shouldBe 7
        all.toList() shouldBe expected
    }

    @Test
    fun title() {
        // Given
        val samples = SampleKB.entries

        // When
        val titles = samples.map { it.title() }

        // Then
        titles shouldBe listOf(
            "Thyroid Stimulating Hormone",
            "Thyroid Stimulating Hormone - cases only",
            "Contact Lens Prescription",
            "Contact Lens Prescription - cases only",
            "Zoo Animals",
            "Zoo Animals - cases only",
            "Pathology"
        )
    }

    @Test
    fun demonstrations() {
        // Given
        val expected = listOf(TSH, CONTACT_LENSES, ZOO, PATHOLOGY)

        // When
        val demonstrations = SampleKB.demonstrations()

        // Then
        demonstrations shouldBe expected
    }

    @Test
    fun description() {
        // Given
        val samples = SampleKB.entries

        // When
        val descriptions = samples.map { it.description() }

        // Then
        descriptions shouldBe listOf(
            "Interpretative comments for thyroid function test reports, from a published paper. An example only, not for advice or diagnosis.",
            "The thyroid function test cases only, with no rules.",
            "Contact lens prescription rules from a UNSW course on machine learning and Ripple-Down Rules. An example only.",
            "The contact lens cases only, with no rules.",
            "Classifies animals from their features; the cases and rules from Chapter 5 of Compton and Kang's book on Ripple-Down Rules.",
            "The zoo animal cases only, with no rules.",
            "Three small pathology cases for trying out rule building, cornerstone review, derived attributes and the AI report. No rules are built yet."
        )
        descriptions.forEach { it.lines().size shouldBe 1 }
    }

    @Test
    fun jsonSerialisation() {
        TSH_CASES shouldBe serializeDeserialize(TSH_CASES)
        TSH shouldBe serializeDeserialize(TSH)
    }

    private fun serializeDeserialize(sampleKB: SampleKB): SampleKB {
        val serialized = Json.encodeToString(sampleKB)
        return Json.decodeFromString(serialized)
    }
}
