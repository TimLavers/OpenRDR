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
            "Contact Lense Prescription",
            "Contact Lense Prescription - cases only",
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
    fun jsonSerialisation() {
        TSH_CASES shouldBe serializeDeserialize(TSH_CASES)
        TSH shouldBe serializeDeserialize(TSH)
    }

    private fun serializeDeserialize(sampleKB: SampleKB): SampleKB {
        val serialized = Json.encodeToString(sampleKB)
        return Json.decodeFromString(serialized)
    }
}
