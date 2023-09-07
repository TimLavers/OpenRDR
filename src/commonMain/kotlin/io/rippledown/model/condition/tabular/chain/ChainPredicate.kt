package io.rippledown.model.condition.tabular.chain

import kotlinx.serialization.Serializable

@Serializable
sealed interface ChainPredicate {

    fun matches(pattern: List<Boolean>): Boolean

    fun description(): String

    fun plurality(): Boolean = false
}