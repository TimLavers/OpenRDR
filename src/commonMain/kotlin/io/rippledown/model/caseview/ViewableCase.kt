package io.rippledown.model.caseview

import io.rippledown.model.Attribute
import kotlinx.serialization.Serializable
import io.rippledown.model.RDRCase

@Serializable
data class ViewableCase(val rdrCase: RDRCase, val viewProperties: CaseViewProperties) {
    val name = rdrCase.name
    val dates = rdrCase.dates
    val interpretation = rdrCase.interpretation

    fun attributes(): List<Attribute> {
        return viewProperties.orderAttributes(rdrCase.attributes)
    }
}