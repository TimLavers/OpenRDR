package io.rippledown.textdiff

import io.rippledown.model.diff.*
import kotlinx.serialization.Serializable

@Serializable
sealed interface Fragment {
    fun toDiff() = when (this) {
        is UnchangedFragment -> Unchanged(text)
        is AddedFragment -> Addition(added)
        is RemovedFragment -> Removal(removed)
        is ReplacedFragment -> Replacement(original, replacement)
    }
}

@Serializable
data class UnchangedFragment(val text: String) : Fragment

@Serializable
data class AddedFragment(val added: String) : Fragment

@Serializable
data class RemovedFragment(val removed: String) : Fragment

@Serializable
data class ReplacedFragment(val original: String, val replacement: String) : Fragment

@Serializable
data class FragmentList(val fragments: List<Fragment> = listOf()) {
    fun toDiffList() = DiffList(fragments.map { it.toDiff() })
}

