package io.rippledown.model.chat

import kotlinx.serialization.Serializable

@Serializable
data class CapabilitySection(val heading: String, val items: List<String>)
