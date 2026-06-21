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
    val unresolvedRanges: List<IntRangeData> = emptyList()
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

    fun truncatedText() = if(text.length <= 20) text else "${text.substring(0, 20)}..."

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