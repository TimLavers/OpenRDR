package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.persistence.OrderStore

interface ConclusionProvider : EntityProvider<Conclusion> {
    fun getOrCreate(text: String, variables: List<CommentVariable> = emptyList()): Conclusion
}

class InterpretationViewManager(
    conclusionOrderStore: OrderStore,
    conclusionProvider: ConclusionProvider,
    private val attributeProvider: EntityProvider<io.rippledown.model.Attribute>
) :
    OrderedEntityManager<Conclusion>(conclusionOrderStore, conclusionProvider) {

    fun viewableInterpretation(interpretation: Interpretation, case: RDRCase): ViewableInterpretation {
        require(interpretation.caseId.id != null) {
            "Cannot create a viewable interpretation if the case does not have an id."
        }
        val orderedConclusions = inOrder(interpretation.conclusions())
        val textFromOrderedConclusions = orderedConclusions.joinToString(COMMENT_SEPARATOR) { it.text }
        val renderedComments = orderedConclusions.map { conclusion ->
            conclusion.render(case) { id ->
                attributeProvider.getById(id)
            }
        }
        return ViewableInterpretation(
            interpretation,
            textGivenByRules = textFromOrderedConclusions,
            renderedComments = renderedComments
        )
    }
}