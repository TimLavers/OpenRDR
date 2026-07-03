package io.rippledown.model.report

import kotlinx.serialization.Serializable

@Serializable
data class CaseReport(
    val markdown: String,
    val generated: Boolean = true // false when there were no comments to report on
)
