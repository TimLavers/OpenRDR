package io.rippledown.model.diff

import kotlinx.serialization.Serializable

/**
 * The pending change to a derived attribute in the rule session currently in
 * progress, so that the Derived attributes panel can preview it. This is the
 * derived-attribute analogue of [Diff], which previews pending comment changes.
 *
 * The pending value is not present anywhere on the client during a session,
 * because the rule tree has not been changed yet, so the server evaluates the
 * value expression against the session case and sends the result here.
 */
@Serializable
sealed interface DerivedValueChange {
    val attributeName: String
}

/**
 * The rule being built will assign [value] to a derived attribute that has no
 * value on the case yet. [value] is empty if the expression cannot be evaluated
 * against the case, for example because an attribute it references has no value.
 */
@Serializable
data class DerivedValueAddition(
    override val attributeName: String = "",
    val value: String = "",
    val formula: String = ""
) : DerivedValueChange

/**
 * The rule being built will retract the current value of the attribute.
 */
@Serializable
data class DerivedValueRemoval(
    override val attributeName: String = ""
) : DerivedValueChange

/**
 * The rule being built will change the attribute's value from its current one
 * to [newValue].
 */
@Serializable
data class DerivedValueReplacement(
    override val attributeName: String = "",
    val newValue: String = "",
    val newFormula: String = ""
) : DerivedValueChange
