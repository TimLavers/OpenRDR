package io.rippledown.kb.chat

import io.rippledown.model.CommentVariable
import io.rippledown.model.VARIABLE_TOKEN
import kotlinx.serialization.Serializable

@Serializable
data class ChatCommentVariable(
    val attributeName: String? = null
)

/** Matches a `{attributeName}` placeholder in a comment. */
val COMMENT_PLACEHOLDER_REGEX = Regex("\\{[^}]*\\}")

/**
 * Convert an LLM-facing comment containing `{attributeName}` placeholders into its internal form
 * (placeholders replaced by [VARIABLE_TOKEN]) together with the resolved comment variables.
 *
 * The model occasionally supplies more variables than there are placeholders — e.g. attaching a
 * variable to a comment that merely mentions an attribute name but contains no placeholder. Variables
 * are therefore aligned to the number of placeholders actually present, so a comment with no
 * placeholders carries no variables.
 */
fun resolveCommentVariables(
    comment: String,
    variables: List<ChatCommentVariable>,
    ruleService: RuleService
): Pair<String, List<CommentVariable>> {
    val placeholderCount = COMMENT_PLACEHOLDER_REGEX.findAll(comment).count()
    val internalComment = comment.replace(COMMENT_PLACEHOLDER_REGEX, Regex.escapeReplacement(VARIABLE_TOKEN))
    val resolvedVariables = variables.take(placeholderCount).toCommentVariables(ruleService)
    return internalComment to resolvedVariables
}

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
