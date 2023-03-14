package io.rippledown.textdiff

import com.github.difflib.text.DiffRowGenerator
import io.rippledown.model.Conclusion
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToRemoveConclusion
import io.rippledown.model.rule.RuleTreeChange

sealed interface Revision {
    fun toRuleAction(): RuleTreeChange
}

data class AddedFragment(val addedText: String) : Revision {
    override fun toRuleAction(): RuleTreeChange {
        return ChangeTreeToAddConclusion(Conclusion(addedText))
    }
}

data class RemovedFragment(val removedText: String) : Revision {
    override fun toRuleAction(): RuleTreeChange {
        return ChangeTreeToRemoveConclusion(Conclusion(removedText))
    }
}

// Unicode characters for start and end of added and removed text fragments that are unlikely to occur in the text.
const val START_ADD = "``"
const val START_REMOVE = "~~"
const val END_ADD = "``"
const val END_REMOVE = "~~"

/*
const val START_ADD = '\u0391'.toString()
const val START_REMOVE = '\u0392'.toString()
const val END_ADD = '\u03B1'.toString()
const val END_REMOVE = '\u03B2'.toString()

*/
fun revisions(originalFragments: List<String>, revisedText: String): List<Revision> {
    val mergeGenerator: DiffRowGenerator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .inlineDiffByWord(true)
        .mergeOriginalRevised(true)
        .oldTag { f -> if (f) START_REMOVE else END_REMOVE }
        .newTag { f -> if (f) START_ADD else END_ADD }
        .build()


    val generator: DiffRowGenerator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .inlineDiffByWord(true)
        .oldTag { f -> if (f) START_REMOVE else END_REMOVE }
        .newTag { f -> if (f) START_ADD else END_ADD }
        .build()

    val originalText = originalFragments.joinToString(" ")
    generator.generateDiffRows(listOf(originalText), listOf(revisedText)).forEach {
        println("old line        = ${it.oldLine}")
        println("new line        = ${it.newLine}")
    }
    val diffRow = generator.generateDiffRows(listOf(originalText), listOf(revisedText))[0]

    mergeGenerator.generateDiffRows(listOf(originalText), listOf(revisedText)).forEach {
        println("merged old line = ${it.oldLine}")
        println("merged new line = ${it.newLine}")
    }

    val pattern = "([^$START_ADD$START_REMOVE$END_ADD$END_REMOVE]+)"
    val addRegex = Regex("$START_ADD$pattern$END_ADD")
    val removeRegex = Regex("$START_REMOVE$pattern$END_REMOVE")

    val revisions = mutableListOf<Revision>()
    for (match in addRegex.findAll(diffRow.newLine)) {
        val addedText = match.groupValues[1].trim()
        revisions.add(AddedFragment(addedText))
    }
    for (match in removeRegex.findAll(diffRow.oldLine)) {
        val removedText = match.groupValues[1].trim()
        revisions.add(RemovedFragment(removedText))
    }
    return revisions
}