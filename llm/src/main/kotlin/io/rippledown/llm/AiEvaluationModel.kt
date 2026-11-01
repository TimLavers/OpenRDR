package io.rippledown.llm

/** Model selection for new definitions and model evaluation; existing definitions keep their saved profile. */
fun aiEvaluationModel(modelOverride: String? = System.getenv("AI_EVALUATION_MODEL")): String =
    modelOverride ?: GEMINI_MODEL
