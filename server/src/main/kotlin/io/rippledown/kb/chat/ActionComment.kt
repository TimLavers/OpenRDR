package io.rippledown.kb.chat

import kotlinx.serialization.Serializable

@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val debug: String? = null,
    val comment: String? = null,
    val replacementComment: String? = null,
    val reasons: List<String>? = null,
)