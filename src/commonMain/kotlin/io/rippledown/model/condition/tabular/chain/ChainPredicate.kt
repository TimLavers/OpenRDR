package io.rippledown.model.condition.tabular.chain

import kotlinx.serialization.Serializable

@Serializable
sealed interface ChainPredicate {
    abstract val id: Int?

    fun matches(pattern: List<Boolean>): Boolean
}