package io.rippledown.kb

import io.rippledown.model.Conclusion
import io.rippledown.persistence.OrderStore

typealias ConclusionProvider = EntityProvider<Conclusion>

class InterpretationViewManager(conclusionOrderStore: OrderStore, conclusionProvider: ConclusionProvider) :
    OrderedEntityManager<Conclusion>(conclusionOrderStore, conclusionProvider) {

    /**
     * Insert the conclusions into the view ordering, maintaining their relative order if it is consistent with the existing view ordering.
     */
    fun insert(conclusions: List<Conclusion>) {
// a b c      b p a ->

    }
}