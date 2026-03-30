package io.rippledown.model.rule

import io.rippledown.model.diff.Diff
import kotlinx.serialization.Serializable

/**
 * A request to build a complete rule without using the UI.
 *
 * @param caseName The name of the processed case to build the rule for
 * @param diff The change to make (Addition, Removal, or Replacement)
 * @param conditions Condition expressions in human-readable text form, parsed on the server
 */
@Serializable
data class BuildRuleRequest(
    val caseName: String,
    val diff: Diff,
    val conditions: List<String>
)
