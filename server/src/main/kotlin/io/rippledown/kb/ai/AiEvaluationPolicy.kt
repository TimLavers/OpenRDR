package io.rippledown.kb.ai

import io.rippledown.model.rule.AiEvaluationProfile

/**
 * The evaluation profiles this server can honour. A definition stores its
 * profile, so a server must refuse one it cannot evaluate as specified
 * rather than quietly substitute its own provider or prompt contract.
 */
object AiEvaluationPolicy {
    const val GEMINI = "gemini"
    const val PROMPT_VERSION = 1
    const val SCHEMA_VERSION = 1

    fun defaultProfile(model: String) = AiEvaluationProfile(
        provider = GEMINI,
        model = model,
        temperature = 0.0,
        promptVersion = PROMPT_VERSION,
        schemaVersion = SCHEMA_VERSION
    )

    fun supports(profile: AiEvaluationProfile) =
        profile.provider == GEMINI &&
                profile.promptVersion == PROMPT_VERSION &&
                profile.schemaVersion == SCHEMA_VERSION
}
