package io.rippledown.model.condition.tabular.chain

import kotlinx.serialization.Serializable

@Serializable
data object Current: ChainPredicate {

    override fun matches(pattern: List<Boolean>) = if (pattern.isEmpty()) false else pattern.last()

    override fun description() = ""
}