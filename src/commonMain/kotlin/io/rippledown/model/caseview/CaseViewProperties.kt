package io.rippledown.model.caseview

import kotlinx.serialization.Serializable
import io.rippledown.model.Attribute

@Serializable
data class CaseViewProperties(val attributes: List<Attribute>)