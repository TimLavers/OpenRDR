package io.rippledown.model.chat

import kotlinx.serialization.Serializable

const val SUMMARY_MAX_LENGTH = 120

@Serializable
data class KnowledgeBaseListing(
    val storedNames: List<String>,
    val demonstrationNames: List<String>,
    val openName: String? = null,
    val descriptions: Map<String, String> = emptyMap()
)

fun summaryOf(description: String): String {
    val firstLine = description.lines().firstOrNull { it.isNotBlank() }?.trim()?.trimStart('#')?.trim() ?: ""
    return if (firstLine.length <= SUMMARY_MAX_LENGTH) firstLine
    else firstLine.take(SUMMARY_MAX_LENGTH - 1) + "…"
}
