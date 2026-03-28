package io.rippledown.model.rule

import io.rippledown.model.diff.Diff
import kotlinx.serialization.Serializable

/**
 * A request to build a complete rule without using the UI.
 * All conditions are "Is" predicates with "Current" signature.
 *
 * @param caseName The name of the processed case to build the rule for
 * @param diff The change to make (Addition, Removal, or Replacement)
 * @param conditions Pairs of (attributeName, value) for Is-predicate conditions
 */
@Serializable
data class BuildRuleRequest(
    val caseName: String,
    val diff: Diff,
    val conditions: List<ConditionDescriptor>
)

@Serializable
data class ConditionDescriptor(val attributeName: String, val value: String)
