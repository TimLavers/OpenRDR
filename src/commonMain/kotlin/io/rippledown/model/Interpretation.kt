package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class Interpretation(val caseId: CaseId, val text: String)