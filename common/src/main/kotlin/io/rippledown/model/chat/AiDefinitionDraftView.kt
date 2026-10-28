package io.rippledown.model.chat

import io.rippledown.model.rule.AiBooleanExample
import kotlinx.serialization.Serializable

/**
 * The structured content of an AI definition draft offer, attached to a
 * [ChatResponse] so the client can render the draft as a labelled card rather
 * than a block of text. [previewValue] is the evaluated value; when no
 * evaluation was possible, [previewMessage] explains why instead.
 */
@Serializable
data class AiDefinitionDraftView(
    val attributeName: String,
    val source: String,
    val criteria: String,
    val examples: List<AiBooleanExample> = emptyList(),
    val previewValue: String? = null,
    val previewMessage: String? = null,
    val evidence: String? = null,
    val changesEverywhere: Boolean = false,
    val prompt: String? = null
) {
    /** The preview lines as they appear in the response text. */
    val previewText: String
        get() = if (previewValue != null) {
            "Preview: $previewValue" + (evidence?.let { "\nEvidence: \"$it\"" } ?: "")
        } else {
            previewMessage.orEmpty()
        }
}
