package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class CasesInfo(val count: Int, val resourcePath: String)