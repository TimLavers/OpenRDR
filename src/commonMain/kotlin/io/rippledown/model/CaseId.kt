package io.rippledown.model

import kotlinx.serialization.Serializable

enum class CaseType {
    Cornerstone,
    Processed
}

@Serializable
data class CaseId(val id: Long?, val name: String, val type: CaseType = CaseType.Processed) {
    constructor(name: String): this(null, name)
}