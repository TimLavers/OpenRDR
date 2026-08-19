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
sealed interface Diff : PendingChange {
    fun left(): String
    fun right(): String

    /**
     * The name of the comment attribute the change concerns, shown in the
     * name column of the Comments panel while the change is pending. For a
     * replacement it is the name of the replacing attribute, which is the
     * one the rule being built assigns. Empty when the diff was not made
     * for a comment attribute, as in the character-level diffing of
     * DiffGenerator.
     */
    val attributeName: String
}

@Serializable
data class Replacement(
    val originalText: String = "",
    val replacementText: String = "",
    override val attributeName: String = ""
) : Diff {
    override fun left() = originalText
    override fun right() = replacementText
}

@Serializable
data class Addition(val addedText: String = "", override val attributeName: String = "") : Diff {
    override fun left() = ""
    override fun right() = addedText
}

@Serializable
data class Removal(val removedText: String = "", override val attributeName: String = "") : Diff {
    override fun left() = removedText
    override fun right() = ""
}




