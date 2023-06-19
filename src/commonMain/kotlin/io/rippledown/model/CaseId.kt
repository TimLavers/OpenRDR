package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class CaseId(val id: Long?, val name: String) {
    constructor(name: String): this(null, name)
}