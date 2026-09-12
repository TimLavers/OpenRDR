package io.rippledown.model.chat

import kotlinx.serialization.Serializable

@Serializable
data class KnowledgeBaseListing(
    val storedNames: List<String>,
    val demonstrationNames: List<String>,
    val openName: String? = null
)
