package io.rippledown.kb

import io.rippledown.model.Conclusion
import io.rippledown.persistence.OrderStore

typealias ConclusionProvider = EntityProvider<Conclusion>

class InterpretationViewManager(conclusionOrderStore: OrderStore, conclusionProvider: ConclusionProvider) :
    OrderedEntityManager<Conclusion>(conclusionOrderStore, conclusionProvider)