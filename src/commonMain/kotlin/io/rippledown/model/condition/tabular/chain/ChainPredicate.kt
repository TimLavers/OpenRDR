package io.rippledown.model.condition.tabular.chain

import kotlinx.serialization.Serializable

@Serializable
sealed interface ChainPredicate {

    fun matches(pattern: List<Boolean>): Boolean

    fun description(): String

    fun plurality(): Boolean = false
}

@Serializable
data object Current: ChainPredicate {

    override fun matches(pattern: List<Boolean>) = if (pattern.isEmpty()) false else pattern.last()

    override fun description() = ""
}

@Serializable
data object All: ChainPredicate {

    override fun matches(pattern: List<Boolean>) = if (pattern.isEmpty()) false else !pattern.contains(false)

    override fun description() = "all"

    override fun plurality() = true
}