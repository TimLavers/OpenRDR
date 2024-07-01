package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.defaultDate

fun case(vararg pairs: Pair<Attribute, String>, name: String = "SessionCase"): RDRCase {
    val builder = RDRCaseBuilder()
    pairs.forEach {
        builder.addValue(it.first, defaultDate, it.second)
    }
    return builder.build(name)
}