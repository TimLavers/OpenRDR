package io.rippledown.model.diff

import kotlinx.serialization.Serializable

/**
 * Used in the GUI to show the changes in the sentences of the interpretative report and the
 * sentences in the changed report.
 *
 * Also used in the DiffGenerator to calculate differences in the lists of single characters
 * corresponding to the sentences
 */

@Serializable
sealed interface Diff {
    fun left(): String
    fun right(): String
}

@Serializable
data class Unchanged(val originalText: String = "") : Diff {
    override fun left() = originalText
    override fun right() = originalText
}

@Serializable
data class Replacement(val originalText: String = "", val replacementText: String = "") : Diff {
    override fun left() = originalText
    override fun right() = replacementText
}

@Serializable
data class Addition(val addedText: String = "") : Diff {
    override fun left() = ""
    override fun right() = addedText
}

@Serializable
data class Removal(val removedText: String = "") : Diff {
    override fun left() = removedText
    override fun right() = ""
}




