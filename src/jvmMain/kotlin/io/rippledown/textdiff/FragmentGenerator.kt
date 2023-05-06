package io.rippledown.textdiff

import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*

private val REGEX = "(?<=\\.\\s)".toRegex()

fun diffList(interpretation: Interpretation) = fragmentList(interpretation).toDiffList()

internal fun fragmentList(interpretation: Interpretation): FragmentList {
    val original = interpretation.textGivenByRules()
    val originalSentences = original.splitIntoSentences()
    val changed = interpretation.verifiedText

    return if (changed == null) {
        FragmentList(originalSentences.map { UnchangedFragment(it) })
    } else {
        val changedSentences = changed.splitIntoSentences()
        val fragments = generateDifferences(originalSentences, changedSentences)
        FragmentList(fragments)
    }
}

internal fun String.splitIntoSentences() = if (isBlank()) emptyList() else split(REGEX).map { it.trim() }

internal fun generateDifferences(
    originalTexts: List<String>,
    changedTexts: List<String>
): List<Fragment> {
    val mapper = TextToAlphabetMapper()

    val alphabetStringForOriginalTexts = originalTexts.toAlphabetString(mapper)
    val alphabetStringForChangedTexts = changedTexts.toAlphabetString(mapper)

    //calculate the diff between the two texts in character array form
    val alphabetDiffs = diffs(alphabetStringForOriginalTexts, alphabetStringForChangedTexts)

    //convert the diff to a list of fragments, i.e. with the original texts
    return alphabetDiffs.toFragmentList(mapper)
}

internal fun List<String>.toAlphabetString(mapper: TextToAlphabetMapper) = joinToString("") { mapper.toAlpha(it) }

internal fun List<Diff>.toFragmentList(mapper: TextToAlphabetMapper): List<Fragment> {
    return map { it.toFragment(mapper) }
}

internal fun Diff.toFragment(mapper: TextToAlphabetMapper) = when (this) {
    is Unchanged -> UnchangedFragment(mapper.toText(left()))
    is Addition -> AddedFragment(mapper.toText(right()))
    is Removal -> RemovedFragment(mapper.toText(left()))
    is Replacement -> ReplacedFragment(mapper.toText(left()), mapper.toText(right()))
}

