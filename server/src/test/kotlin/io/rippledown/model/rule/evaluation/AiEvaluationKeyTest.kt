package io.rippledown.model.rule.evaluation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.rippledown.model.Attribute
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiBooleanExample
import io.rippledown.model.rule.AiEvaluationProfile
import kotlin.test.Test

internal class AiEvaluationKeyTest {
    private val notes = Attribute(1, "clinical_notes")
    private val profile = AiEvaluationProfile("gemini", "gemini-2.5-flash", 0.0, 1, 1)
    private val hasDiabetes = AiBoolean(
        sourceAttribute = notes,
        instructions = "True if the notes affirm a personal history of diabetes.",
        examples = listOf(AiBooleanExample("FH diab", false, "Family history only")),
        revision = "rev-1",
        evaluationProfile = profile
    )
    private val request = AiEvaluationRequest(hasDiabetes, "T2 Diab")

    @Test
    fun `the key is the revision, the definition fingerprint and the input hash`() {
        // When the key of a request is taken
        val key = request.key()

        // Then its parts are the revision and two SHA-256 hex digests
        key.revision shouldBe "rev-1"
        key.definitionFingerprint shouldMatch "[0-9a-f]{64}"
        key.inputHash shouldMatch "[0-9a-f]{64}"
        key.inputHash shouldBe sha256Hex("T2 Diab")
    }

    @Test
    fun `the input hash is of the exact bytes`() {
        // Given inputs differing only in whitespace
        // Then they hash differently: nonblank text is never normalised
        AiEvaluationRequest(hasDiabetes, "T2 Diab").key().inputHash shouldNotBe
                AiEvaluationRequest(hasDiabetes, " T2 Diab").key().inputHash
        sha256Hex("abc") shouldBe "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }

    @Test
    fun `the same request always gives the same key`() {
        request.key() shouldBe AiEvaluationRequest(hasDiabetes.copy(), "T2 Diab").key()
    }

    @Test
    fun `renaming the source does not change the fingerprint`() {
        val renamed = hasDiabetes.alignAttributes { Attribute(1, "Notes") }
        renamed.canonicalFingerprint() shouldBe hasDiabetes.canonicalFingerprint()
    }

    @Test
    fun `the revision is not part of the fingerprint but is part of the key`() {
        val revised = hasDiabetes.copy(revision = "rev-2")
        revised.canonicalFingerprint() shouldBe hasDiabetes.canonicalFingerprint()
        AiEvaluationRequest(revised, "T2 Diab").key() shouldNotBe request.key()
    }

    @Test
    fun `semantic edits change the fingerprint`() {
        val base = hasDiabetes.canonicalFingerprint()
        hasDiabetes.copy(sourceAttribute = Attribute(2, "clinical_notes")).canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(instructions = "True if the notes mention diabetes.").canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(examples = emptyList()).canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(examples = listOf(AiBooleanExample("FH diab", true, "Family history only")))
            .canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(examples = listOf(AiBooleanExample("FH diab", false)))
            .canonicalFingerprint() shouldNotBe base
    }

    @Test
    fun `profile edits change the fingerprint`() {
        val base = hasDiabetes.canonicalFingerprint()
        hasDiabetes.copy(evaluationProfile = profile.copy(model = "gemini-2.5-pro")).canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(evaluationProfile = profile.copy(temperature = 0.5)).canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(evaluationProfile = profile.copy(promptVersion = 2)).canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(evaluationProfile = profile.copy(schemaVersion = 2)).canonicalFingerprint() shouldNotBe base
        hasDiabetes.copy(evaluationProfile = profile.copy(provider = "other")).canonicalFingerprint() shouldNotBe base
    }

    @Test
    fun `example order is significant`() {
        // Given the same examples in a different order; the prompt presents them in order, so the identity differs
        val swapped = hasDiabetes.copy(
            examples = listOf(AiBooleanExample("T2 Diab", true), AiBooleanExample("FH diab", false, "Family history only"))
        )
        val original = hasDiabetes.copy(
            examples = listOf(AiBooleanExample("FH diab", false, "Family history only"), AiBooleanExample("T2 Diab", true))
        )
        swapped.canonicalFingerprint() shouldNotBe original.canonicalFingerprint()
    }

    @Test
    fun `the text form shows the three parts`() {
        val key = request.key()
        key.asString() shouldBe "rev-1:${key.definitionFingerprint}:${key.inputHash}"
    }
}
