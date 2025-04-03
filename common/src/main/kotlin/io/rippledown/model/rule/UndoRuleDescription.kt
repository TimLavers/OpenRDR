package io.rippledown.model.rule

import kotlinx.serialization.Serializable

@Serializable
data class UndoRuleDescription(val description: String, val canRemove: Boolean)