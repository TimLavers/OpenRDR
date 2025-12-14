package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerChatResult(val userMessage: String, val kbInfo: KBInfo? = null)