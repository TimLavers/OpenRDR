package io.rippledown.kb

import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.persistence.OrderStore
import io.rippledown.persistence.VerifiedTextStore

typealias ConclusionProvider = EntityProvider<Conclusion>

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
        return ViewableInterpretation(interpretation, verifiedText)
    }
}