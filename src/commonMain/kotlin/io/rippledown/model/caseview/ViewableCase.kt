package io.rippledown.model.caseview

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class ViewableCase(val rdrCase: RDRCase, val viewProperties: CaseViewProperties = CaseViewProperties()) {
    val name = rdrCase.name
    val dates = rdrCase.dates
    val interpretation = rdrCase.interpretation

    init {
        check(rdrCase.attributes == viewProperties.attributes.toSet()) {
            "Case attributes do not match view properties attributes"
        }
    }

    fun attributes(): List<Attribute> {
        return viewProperties.attributes
    }
}