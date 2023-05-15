package io.rippledown.model.caseview

import io.rippledown.model.Attribute
import kotlinx.serialization.Serializable

@Serializable
data class CaseViewProperties(val attributes: List<Attribute> = emptyList())