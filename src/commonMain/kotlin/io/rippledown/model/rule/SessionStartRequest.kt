package io.rippledown.model.rule

import io.rippledown.model.diff.Diff
import kotlinx.serialization.Serializable

/**
 * This is the information that is sent from the GUI tp the server to start a rule session
 */
@Serializable
data class SessionStartRequest(
    val caseId: String = "",
    val diff: Diff
)
