package io.rippledown.model.condition.tabular.chain

import kotlinx.serialization.Serializable

@Serializable
data class Current(override val id: Int? = null): ChainPredicate {

    override fun matches(pattern: List<Boolean>) =  if (pattern.isEmpty()) false else pattern.last()

    override fun description() = ""
}