package io.rippledown.model

import kotlinx.serialization.Serializable

const val VARIABLE_TOKEN = "\${}"

@Serializable
data class CommentVariable(
    val attributeId: Int
)

@Serializable
data class RenderedComment(
    val text: String,
    val unresolvedRanges: List<IntRangeData> = emptyList(),
    // The texts of the conditions of the rules that gave the comment, for
    // display in the comment's tooltip.
    val conditions: List<String> = emptyList()
)

@Serializable
data class IntRangeData(val start: Int, val endInclusive: Int) {
    fun toIntRange() = start..endInclusive
}

/**
 * The maximum length of a comment.
 */
const val MAXIMUM_COMMENT_LENGTH = 2048

/**
 * The given comment text truncated to at most 20 characters, appending
 * "..." if truncated, for use in confirmation messages. If the truncation
 * point falls inside a `{attributeName}` variable rendering, the whole
 * variable is kept.
 */
fun String.truncatedComment(): String {
    if (length <= 20) return this
    var truncated = substring(0, 20)
    val openBraceIndex = truncated.lastIndexOf('{')
    if (openBraceIndex != -1 && !truncated.substring(openBraceIndex).contains('}')) {
        val closingBraceIndex = indexOf('}', 20)
        if (closingBraceIndex != -1) {
            truncated = substring(0, closingBraceIndex + 1)
        }
    }
    return "$truncated..."
}