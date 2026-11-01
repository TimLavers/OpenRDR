package io.rippledown.kb.ai

import io.rippledown.llm.GEMINI_BOOLEAN_PROMPT_VERSION
import io.rippledown.llm.GEMINI_BOOLEAN_PROVIDER
import io.rippledown.llm.GEMINI_BOOLEAN_SCHEMA_VERSION
import io.rippledown.model.rule.AiEvaluationProfile

/**
 * The evaluation profiles this server can honour. A definition stores its
 * profile, so a server must refuse one it cannot evaluate as specified
 * rather than quietly substitute its own provider or prompt contract.
 * The versions are those of the installed Gemini classifier.
 */
object AiEvaluationPolicy {
    const val GEMINI = GEMINI_BOOLEAN_PROVIDER
    const val PROMPT_VERSION = GEMINI_BOOLEAN_PROMPT_VERSION
    const val SCHEMA_VERSION = GEMINI_BOOLEAN_SCHEMA_VERSION

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
