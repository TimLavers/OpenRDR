package io.rippledown.model.caseview

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.utils.serializeDeserialize
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class DerivedValueStatusTest {
    private val provenance = AiProvenance(
        sourceName = "clinical_notes",
        instructions = "True if the notes affirm a personal history of diabetes.",
        evidence = "T2 Diab",
        evaluatedAt = 1_700_000_000_000,
        model = "gemini-2.5-flash",
        revision = "rev-1"
    )

    @Test
    fun `each status round trips as the sealed type`() {
        // Given one of each status
        val statuses: List<DerivedValueStatus> = listOf(
            DerivedValueStatus.Evaluated,
            DerivedValueStatus.NoInput("clinical_notes"),
            DerivedValueStatus.Failed("Gemini unavailable")
        )

        // When each is serialized and deserialized
        // Then it is unchanged
        statuses.forEach { serializeDeserialize(it) shouldBe it }
    }

    @Test
    fun `statuses carry a discriminator`() {
        Json.encodeToString<DerivedValueStatus>(DerivedValueStatus.NoInput("notes")) shouldContain "\"type\":\"NoInput\""
    }

    @Test
    fun `no input and failure are distinct from each other and from a value`() {
        // Given a no-input and a failed status
        val noInput: DerivedValueStatus = DerivedValueStatus.NoInput("notes")
        val failed: DerivedValueStatus = DerivedValueStatus.Failed("timeout")

        // Then neither is the evaluated status and they differ from each other
        noInput shouldNotBe DerivedValueStatus.Evaluated
        failed shouldNotBe DerivedValueStatus.Evaluated
        noInput shouldNotBe failed
    }

    @Test
    fun `provenance round trips with and without evidence`() {
        serializeDeserialize(provenance) shouldBe provenance
        val withoutEvidence = provenance.copy(evidence = null)
        serializeDeserialize(withoutEvidence) shouldBe withoutEvidence
    }
}
