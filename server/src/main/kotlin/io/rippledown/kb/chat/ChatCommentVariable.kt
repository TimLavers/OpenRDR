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
 * There is exactly one variable per placeholder, whatever the model supplied in [variables]: a
 * placeholder names its attribute, so the name is taken from the placeholder itself, falling back to
 * the variable the model supplied for that placeholder where the placeholder names no attribute.
 * The model's list cannot be relied on — it sometimes supplies more variables than there are
 * placeholders (attaching one to a comment that merely mentions an attribute name), and sometimes
 * fewer or none at all, which would leave a token with no variable to substitute and so render the
 * comment with a raw `${}` in it.
 */
fun resolveCommentVariables(
    comment: String,
    variables: List<ChatCommentVariable>,
    ruleService: RuleService
): Pair<String, List<CommentVariable>> {
    val internalComment = comment.replace(COMMENT_PLACEHOLDER_REGEX, Regex.escapeReplacement(VARIABLE_TOKEN))
    val resolvedVariables = COMMENT_PLACEHOLDER_REGEX.findAll(comment).toList()
        .mapIndexed { index, placeholder ->
            val nameInPlaceholder = placeholder.value.removeSurrounding("{", "}")
            val attributeName = ruleService.attributeForName(nameInPlaceholder)?.name
                ?: variables.getOrNull(index)?.attributeName
            ChatCommentVariable(attributeName)
        }
        .toCommentVariables(ruleService)
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
