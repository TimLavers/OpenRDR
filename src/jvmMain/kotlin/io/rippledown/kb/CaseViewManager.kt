package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.persistence.OrderStore

class CaseViewManager(attributeOrderStore: OrderStore, attributeProvider: AttributeProvider) :
    OrderedEntityManager<Attribute>(attributeOrderStore, attributeProvider) {

    fun getViewableCase(case: RDRCase) = ViewableCase(case, CaseViewProperties(inOrder(case.attributes)))
}