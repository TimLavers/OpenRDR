package io.rippledown.model.rule.evaluation

import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiBooleanExample
import io.rippledown.model.rule.AiEvaluationProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * The version of the canonical representation hashed by
 * [canonicalFingerprint]. Bump it if the representation changes, so that
 * records written under the old one cannot satisfy new requests.
 */
const val AI_FINGERPRINT_VERSION = 1

/**
 * The durable identity of one classification, derived from an
 * [AiEvaluationRequest]: the definition's revision, a content fingerprint
 * of its meaning, and a hash of the exact input text. Display names are
 * not part of it, so a renamed source keeps its records; any semantic or
 * profile edit changes it. KB scope is implicit in the owning store.
 */
data class AiEvaluationKey(
    val revision: String,
    val definitionFingerprint: String,
    val inputHash: String
) {
    fun asString() = "$revision:$definitionFingerprint:$inputHash"
}

fun AiEvaluationRequest.key() = AiEvaluationKey(
    revision = definition.revision,
    definitionFingerprint = definition.canonicalFingerprint(),
    inputHash = sha256Hex(input)
)

/**
 * SHA-256 of a versioned canonical representation of everything that
 * determines how the definition classifies text: source id, criteria,
 * examples in order, and the evaluation profile. Not the source's name
 * and not the revision, which the key carries separately.
 */
fun AiBoolean.canonicalFingerprint(): String {
    val canonical = CanonicalAiDefinition(
        version = AI_FINGERPRINT_VERSION,
        sourceAttributeId = sourceAttribute.id,
        instructions = instructions,
        examples = examples,
        profile = evaluationProfile
    )
    return sha256Hex(canonicalJson.encodeToString(canonical))
}

fun sha256Hex(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

private val canonicalJson = Json { encodeDefaults = true }

@Serializable
private data class CanonicalAiDefinition(
    val version: Int,
    val sourceAttributeId: Int,
    val instructions: String,
    val examples: List<AiBooleanExample>,
    val profile: AiEvaluationProfile
)
