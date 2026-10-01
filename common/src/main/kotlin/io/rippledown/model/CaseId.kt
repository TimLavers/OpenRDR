package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class CaseId(val id: Long?, val name: String = "", val type: CaseListType = CaseListType.Processed) {
    constructor(name: String = "") : this(null, name)
}
