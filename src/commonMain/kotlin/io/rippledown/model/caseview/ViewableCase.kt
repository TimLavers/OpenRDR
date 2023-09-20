package io.rippledown.model.caseview

import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class ViewableCase(val rdrCase: RDRCase, val viewProperties: CaseViewProperties = CaseViewProperties()) {
    val name = rdrCase.caseId.name
    val id = rdrCase.caseId.id
    val dates = rdrCase.dates
    var interpretation = rdrCase.interpretation

    init {
        check(rdrCase.attributes == viewProperties.attributes.toSet()) {
            "Case attributes do not match view properties attributes:\n\nCase attributes: ${rdrCase.attributes}\n\nView properties attributes: ${viewProperties.attributes}"
        }
    }

    fun attributes() = viewProperties.attributes
}