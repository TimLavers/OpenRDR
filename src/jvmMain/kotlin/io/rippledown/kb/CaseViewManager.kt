package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase

class CaseViewManager {
    fun getViewableCase(case: RDRCase): ViewableCase {
        return ViewableCase(case, CaseViewProperties(emptyMap()))
    }

    fun moveJustBelow(moved: Attribute, target: Attribute) {

    }
}