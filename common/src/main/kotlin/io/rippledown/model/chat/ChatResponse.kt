package io.rippledown.model.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val text: String,
    val suggestions: List<String> = emptyList()
)
