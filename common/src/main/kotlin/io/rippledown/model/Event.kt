package io.rippledown.model

import kotlinx.serialization.Serializable

// ORD1
@Serializable
data class Event(val attribute: Attribute, val date: Long)