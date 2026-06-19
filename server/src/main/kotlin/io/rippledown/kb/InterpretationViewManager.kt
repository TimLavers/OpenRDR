package io.rippledown.kb

import io.rippledown.model.COMMENT_SEPARATOR
import io.rippledown.model.CommentVariable
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.persistence.OrderStore

interface ConclusionProvider : EntityProvider<Conclusion> {
    fun getOrCreate(text: String, variables: List<CommentVariable> = emptyList()): Conclusion
}

class InterpretationViewManager(
    conclusionOrderStore: OrderStore,
    conclusionProvider: ConclusionProvider,
) :
    OrderedEntityManager<Conclusion>(conclusionOrderStore, conclusionProvider) {

    fun viewableInterpretation(interpretation: Interpretation): ViewableInterpretation {
        require(interpretation.caseId.id != null) {
            "Cannot create a viewable interpretation if the case does not have an id."
        }
        val orderedConclusions = inOrder(interpretation.conclusions())
        val textFromOrderedConclusions = orderedConclusions.joinToString(COMMENT_SEPARATOR) { it.text }
        return ViewableInterpretation(
            interpretation,
            textGivenByRules = textFromOrderedConclusions
        )
    }
}