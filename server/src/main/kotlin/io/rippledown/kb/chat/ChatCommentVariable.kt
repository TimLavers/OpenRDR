package io.rippledown.kb.chat

import io.rippledown.model.CommentVariable
import kotlinx.serialization.Serializable

@Serializable
data class ChatCommentVariable(
    val attributeName: String? = null
)

/**
 * Resolve the chat-level comment variables (which carry the attribute name supplied by the model)
 * into model-level [CommentVariable]s holding a concrete attribute id. Names are resolved
 * (case-insensitively and tolerant of small misspellings) via [RuleService.attributeForName].
 * Unresolved variables are kept with a sentinel id of -1 so that the placeholder still renders as an
 * unresolved marker rather than being silently dropped.
 */
fun List<ChatCommentVariable>.toCommentVariables(ruleService: RuleService): List<CommentVariable> =
    map { variable ->
        val resolvedId = variable.attributeName
            ?.let { ruleService.attributeForName(it)?.id }
            ?: UNRESOLVED_ATTRIBUTE_ID
        CommentVariable(resolvedId)
    }

const val UNRESOLVED_ATTRIBUTE_ID = -1
