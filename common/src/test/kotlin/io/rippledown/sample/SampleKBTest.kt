package io.rippledown.sample

import io.kotest.matchers.shouldBe
import io.rippledown.sample.SampleKB.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class SampleKBTest {

    @Test
    fun values() {
        val all = SampleKB.entries.toTypedArray()
        all.size shouldBe 6
        all[0] shouldBe TSH
        all[1] shouldBe TSH_CASES
        all[2] shouldBe CONTACT_LENSES
        all[3] shouldBe CONTACT_LENSES_CASES
        all[4] shouldBe ZOO
        all[5] shouldBe ZOO_CASES
    }

    @Test
    fun title() {
        TSH.title() shouldBe "Thyroid Stimulating Hormone"
        TSH_CASES.title() shouldBe "Thyroid Stimulating Hormone - cases only"
        CONTACT_LENSES.title() shouldBe "Contact Lense Prescription"
        CONTACT_LENSES_CASES.title() shouldBe "Contact Lense Prescription - cases only"
        ZOO.title() shouldBe "Zoo Animals"
        ZOO_CASES.title() shouldBe "Zoo Animals - cases only"
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