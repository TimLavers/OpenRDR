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

@Serializable
data class Conclusion(
    val id: Int,
    val text: String,
    val variables: List<CommentVariable> = emptyList()
) {
    init {
        check(text.isNotEmpty()) {
            "Conclusions cannot be blank."
        }
        check(text.length < 2049) {
            "Conclusions have maximum length 2048."
        }
    }

    /**
     * Truncate the conclusion text to at most 20 characters, appending "..." if truncated.
     * If the truncation point falls in the middle of a `${}` variable token, the token is
     * preserved by extending the truncation to include the closing brace.
     *
     * Each `${}` token is replaced with `{attributeName}` using [attributeNameById] before
     * truncation, producing a user-friendly representation suitable for confirmation messages.
     * Conclusions without variables are truncated as plain text.
     */
    fun truncatedText(attributeNameById: (Int) -> String): String {
        val displayText = if (variables.isNotEmpty()) {
            substitutePlaceholders(attributeNameById)
        } else {
            text
        }

        if (displayText.length <= 20) return displayText

        var truncated = displayText.substring(0, 20)

        // If the truncated text ends with "${", extend to include the closing "}"
        if (truncated.endsWith("\${")) {
            val closingBraceIndex = displayText.indexOf('}', 20)
            if (closingBraceIndex != -1) {
                truncated = displayText.substring(0, closingBraceIndex + 1)
            }
        }

        return "$truncated..."
    }

    /**
     * Replace each `${}` token (in order of appearance) with `{attributeName}` using the
     * supplied resolver.
     */
    private fun substitutePlaceholders(attributeNameById: (Int) -> String): String {
        val builder = StringBuilder()
        var pos = 0
        var varIndex = 0
        while (pos < text.length) {
            val tokenIndex = text.indexOf(VARIABLE_TOKEN, pos)
            if (tokenIndex == -1 || varIndex >= variables.size) {
                builder.append(text.substring(pos))
                break
            }
            builder.append(text.substring(pos, tokenIndex))
            builder.append("{${attributeNameById(variables[varIndex].attributeId)}}")
            pos = tokenIndex + VARIABLE_TOKEN.length
            varIndex++
        }
        return builder.toString()
    }

    fun render(
        case: RDRCase,
        attributeById: (Int) -> Attribute?
    ): RenderedComment {
        if (variables.isEmpty()) {
            return RenderedComment(text, emptyList())
        }

        val builder = StringBuilder()
        val unresolvedRanges = mutableListOf<IntRangeData>()
        var textPosition = 0

        variables.forEach { variable ->
            val tokenIndex = text.indexOf(VARIABLE_TOKEN, textPosition)
            if (tokenIndex != -1) {
                // Append text before this variable
                builder.append(text.substring(textPosition, tokenIndex))

                // Resolve the variable
                val attribute = attributeById(variable.attributeId)
                val value = if (attribute != null && case.dates.isNotEmpty()) {
                    case.latestValue(attribute)
                } else {
                    null
                }

                if (value != null && value.isNotBlank()) {
                    // Substitute with the actual value
                    builder.append(value)
                } else {
                    // No value available: render a user-friendly marker (no internal ${} syntax)
                    // and record the range so the UI can highlight it and show an explanatory tooltip.
                    val marker = if (attribute != null) "{${attribute.name}: no value}" else "{no value}"
                    val markerStart = builder.length
                    builder.append(marker)
                    unresolvedRanges.add(IntRangeData(markerStart, builder.length - 1))
                }

                // Skip the placeholder token in the template
                textPosition = tokenIndex + VARIABLE_TOKEN.length
            }
        }

        // Append any remaining text after the last variable
        if (textPosition < text.length) {
            builder.append(text.substring(textPosition))
        }

        return RenderedComment(builder.toString(), unresolvedRanges)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Conclusion

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}