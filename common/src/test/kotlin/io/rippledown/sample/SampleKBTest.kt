package io.rippledown.sample

import io.kotest.matchers.shouldBe
import io.rippledown.sample.SampleKB.TSH
import io.rippledown.sample.SampleKB.TSH_CASES
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class SampleKBTest {

    @Test
    fun values() {
        val all = SampleKB.entries.toTypedArray()
        all.size shouldBe 2
        all[0] shouldBe TSH
        all[1] shouldBe TSH_CASES
    }

    @Test
    fun title() {
        TSH.title() shouldBe "Thyroid Stimulating Hormone"
        TSH_CASES.title() shouldBe "Thyroid Stimulating Hormone - cases only"
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