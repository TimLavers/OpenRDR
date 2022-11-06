package io.rippledown.model.caseview

import io.rippledown.model.Attribute
import kotlinx.serialization.Serializable
import io.rippledown.model.RDRCase
import kotlinx.serialization.Transient

@Serializable
data class ViewableCase(val rdrCase: RDRCase, val viewProperties: CaseViewProperties) {
    val name get() = rdrCase.name
    val dates get() = rdrCase.dates
    val interpretation get() = rdrCase.interpretation

    init {
        check(rdrCase.attributes == viewProperties.attributes.toSet()) {
            "Case attributes do not match view properties attributes"
        }
    }

    fun attributes(): List<Attribute> {
        return viewProperties.attributes
    }
}