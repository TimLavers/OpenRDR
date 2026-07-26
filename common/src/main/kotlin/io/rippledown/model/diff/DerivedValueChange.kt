package io.rippledown.model.diff

import kotlinx.serialization.Serializable

/**
 * The pending change to a derived attribute in the rule session currently in
 * progress, so that the Derived attributes panel can preview it. This is the
 * derived-attribute analogue of [Diff], which previews pending comment changes.
 *
 * A pending change is previewed as the definition the rule will give the
 * attribute, not as a value for the current case. The definition is what the
 * user is confirming, and it has not been given by a rule yet, so evaluating it
 * would show a value that no rule assigns.
 */
@Serializable
sealed interface DerivedValueChange : PendingChange {
    val attributeName: String
}

/**
 * The rule being built will assign the value of [formula] to a derived attribute
 * that has no value on the case yet.
 */
@Serializable
data class DerivedValueAddition(
    override val attributeName: String = "",
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
 * The rule being built will change the attribute's definition from the one that
 * gave its current value to [newFormula].
 */
@Serializable
data class DerivedValueReplacement(
    override val attributeName: String = "",
    val newFormula: String = ""
) : DerivedValueChange
