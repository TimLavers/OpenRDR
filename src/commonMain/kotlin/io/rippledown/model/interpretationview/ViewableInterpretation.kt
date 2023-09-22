package io.rippledown.model.interpretationview

import io.rippledown.model.COMMENT_SEPARATOR
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.DiffList
import kotlinx.serialization.Serializable

@Serializable
data class ViewableInterpretation(
    val interpretation: Interpretation,
    var verifiedText: String? = null,
    var diffList: DiffList = DiffList()
) {

    fun latestText(): String = if (verifiedText != null) verifiedText!! else textGivenByRules()

    fun textGivenByRules(): String {
        return interpretation.ruleSummaries.asSequence().map { it.conclusion?.text }
            .filterNotNull()
            .toMutableSet()//eliminate duplicates
            .toMutableList()
            .sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString(COMMENT_SEPARATOR)
    }

    fun numberOfChanges() = diffList.numberOfChanges()

    fun conditionsForConclusion(conclusion: Conclusion): List<String> {
        return interpretation.ruleSummaries
            .first { ruleSummary -> conclusion == ruleSummary.conclusion }
            .conditionTextsFromRoot
    }

}