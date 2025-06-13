package io.rippledown.persistence

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.utils.today


fun createCase(name: String, attributeToValue: Map<Attribute, String>, id: Long? = null): RDRCase {
    val builder = RDRCaseBuilder()
    attributeToValue.forEach { (attribute, value) -> builder.addResult(attribute, today, TestResult(value)) }
    return builder.build(name, id)
}