package io.rippledown.kb.chat

import kotlinx.serialization.Serializable

@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val debug: String? = null,
    val new_comment: String? = null,
    val existing_comment: String? = null,
    val conditions: List<String>? = null,
)