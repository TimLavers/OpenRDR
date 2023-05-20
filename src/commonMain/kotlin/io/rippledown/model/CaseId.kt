package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class CaseId(val id: String = "", val name: String = id)