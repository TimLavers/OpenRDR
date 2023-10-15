package io.rippledown.model.condition.episodic.signature

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
@Serializable
data object No: ChainPredicate {

    override fun matches(pattern: List<Boolean>) = if (pattern.isEmpty()) true else !pattern.contains(true)

    override fun description() = "no"

    override fun plurality() = false
}

@Serializable
data class AtLeast(val n: Int): ChainPredicate {
    override fun matches(pattern: List<Boolean>) = pattern.filter { it }.size >= n

    override fun description() = "at least $n"

    override fun plurality() = n > 1
}

@Serializable
data class AtMost(val n: Int): ChainPredicate {
    override fun matches(pattern: List<Boolean>) = pattern.filter { it }.size <= n

    override fun description() = "at most $n"

    override fun plurality() = n > 1
}