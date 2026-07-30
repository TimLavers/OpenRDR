package io.rippledown.model.caseview

import kotlinx.serialization.Serializable

/**
 * A non-comment derived attribute value shown in the Derived attributes panel.
 *
 * @param name The attribute name.
 * @param value The value assigned by the KB for the current case.
 * @param formula The formula text (e.g. `Weight / Height ^ 2` or `"diabetic"`).
 * @param conditions The condition texts from root for the rule that assigned
 *   this value, for display in a tooltip.
 */
@Serializable
data class DerivedValueInfo(
    val name: String,
    val value: String,
    val formula: String,
    val conditions: List<String>
)
