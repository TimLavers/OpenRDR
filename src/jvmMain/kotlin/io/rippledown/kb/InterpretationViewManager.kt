package io.rippledown.kb

import io.rippledown.model.COMMENT_SEPARATOR
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.DiffList
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.persistence.OrderStore
import io.rippledown.persistence.VerifiedTextStore
import io.rippledown.textdiff.diffList

typealias ConclusionProvider = EntityProvider<Conclusion>
typealias DiffListGenerator = (original: String, changed: String?) -> DiffList

class InterpretationViewManager(
    conclusionOrderStore: OrderStore,
    conclusionProvider: ConclusionProvider,
    val verifiedTextStore: VerifiedTextStore
) :
    OrderedEntityManager<Conclusion>(conclusionOrderStore, conclusionProvider) {

    fun viewableInterpretation(interpretation: Interpretation): ViewableInterpretation {
        require(interpretation.caseId.id != null) {
            "Cannot create a viewable interpretation if the case does not have an id."
        }

        val verifiedText = verifiedTextStore.get(interpretation.caseId.id)
        val orderedConclusions = inOrder(interpretation.conclusions())
        val textFromOrderedConclusions = orderedConclusions.joinToString(COMMENT_SEPARATOR) { it.text }
        val diffList = diffList(textFromOrderedConclusions, verifiedText)
        return ViewableInterpretation(
            interpretation,
            verifiedText,
            diffList,
            textGivenByRules = textFromOrderedConclusions
        )
    }
}